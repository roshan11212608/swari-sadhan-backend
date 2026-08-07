package swari.sewa.module.employee.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.employee.dto.SalaryRecordDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.entity.SalaryRecord;
import swari.sewa.module.employee.mapper.SalaryRecordMapper;
import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.AttendanceRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.repository.SalaryRecordRepository;
import swari.sewa.module.employee.service.SalaryService;

@Service
@RequiredArgsConstructor
@Transactional
public class SalaryServiceImpl implements SalaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalaryServiceImpl.class);
    
    /** Attendance statuses that count towards paid days. */
    private static final Set<String> PAID_ATTENDANCE_STATUSES =
            Set.of("Present", "Late", "Half Day", "Holiday", "Leave");
    
    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AdvancePaymentRepository advancePaymentRepository;
    private final SalaryRecordMapper salaryRecordMapper;
    
    @Override
    public SalaryRecordDto generateSalary(Long employeeId, int month, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Validate month and year
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }

        // Check if salary record already exists for this month/year
        Optional<SalaryRecord> existingSalary = salaryRecordRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year);

        SalaryRecord salaryRecord;
        if (existingSalary.isPresent()) {
            salaryRecord = existingSalary.get();
        } else {
            salaryRecord = SalaryRecord.builder()
                    .employee(employee)
                    .month(month)
                    .year(year)
                    .paymentStatus("Pending")
                    .build();
        }

        populateSalaryRecord(salaryRecord, employee, month, year);

        // Validate calculated salary components
        if (salaryRecord.getTotalEarnings().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total earnings cannot be negative");
        }
        if (salaryRecord.getTotalDeductions().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total deductions cannot be negative");
        }
        if (salaryRecord.getNetSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Net salary cannot be negative");
        }

        SalaryRecord savedSalary;
        try {
            savedSalary = salaryRecordRepository.save(salaryRecord);
        } catch (DataIntegrityViolationException e) {
            // Handle race condition: another transaction created the record concurrently
            // Fetch the existing record and return it for idempotency
            savedSalary = salaryRecordRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                    .orElseThrow(() -> new ResourceNotFoundException("Salary record not found after concurrent creation"));
        }
        return toDtoEnriched(savedSalary);
    }
    
    private void populateSalaryRecord(SalaryRecord salaryRecord, Employee employee, int month, int year) {
        salaryRecord.setEmployeeName(employee.getFullName());
        salaryRecord.setEmployeePhotoUrl(employee.getProfilePhotoUrl());
        salaryRecord.setEmployeeIdNumber(employee.getEmployeeNumber());
        salaryRecord.setMonthName(Month.of(month).name());
        salaryRecord.setShopName(resolveShopName(employee));
        salaryRecord.setShopLocation(resolveShopLocation(employee));
        salaryRecord.setBasicSalary(employee.getBasicSalary());
        
        int availableDays = getDaysInMonth(month, year);
        int paidDays = Math.min(calculatePaidDays(employee.getId(), month, year), availableDays);
        
        salaryRecord.setAvailableDays(availableDays);
        salaryRecord.setPaidDays(paidDays);
        salaryRecord.setLossOfPayDays(availableDays - paidDays);
        salaryRecord.setAdvanceDeduction(calculateAdvanceDeduction(employee.getId(), month, year));
        
        // Calculate previous balance from latest unpaid month (not sum of all historical)
        java.math.BigDecimal previousBalance = salaryRecordRepository.getLatestPreviousBalanceDueByEmployeeId(employee.getId(), year, month);
        salaryRecord.setPreviousBalance(previousBalance != null ? previousBalance : java.math.BigDecimal.ZERO);
        
        calculateSalaryComponents(salaryRecord);
    }
    
    private String resolveShopName(Employee employee) {
        if (employee == null || employee.getShop() == null) {
            return "";
        }
        return employee.getShop().getName() != null ? employee.getShop().getName() : "";
    }
    
    private String resolveShopLocation(Employee employee) {
        if (employee == null || employee.getShop() == null) {
            return "";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (employee.getShop().getAddressLine1() != null && !employee.getShop().getAddressLine1().isBlank()) {
            parts.add(employee.getShop().getAddressLine1());
        }
        if (employee.getShop().getCity() != null && !employee.getShop().getCity().isBlank()) {
            parts.add(employee.getShop().getCity());
        }
        if (employee.getShop().getState() != null && !employee.getShop().getState().isBlank()) {
            parts.add(employee.getShop().getState());
        }
        return String.join(", ", parts);
    }
    
    /**
     * Maps to a DTO and backfills shop/employee details for legacy records that were
     * generated before these columns existed.
     */
    private SalaryRecordDto toDtoEnriched(SalaryRecord salaryRecord) {
        SalaryRecordDto dto = salaryRecordMapper.toDto(salaryRecord);
        Employee employee = salaryRecord.getEmployee();
        if (employee == null) {
            return dto;
        }
        if (dto.getShopName() == null || dto.getShopName().isBlank()) {
            dto.setShopName(resolveShopName(employee));
        }
        if (dto.getShopLocation() == null || dto.getShopLocation().isBlank()) {
            dto.setShopLocation(resolveShopLocation(employee));
        }
        if (dto.getEmployeeIdNumber() == null || dto.getEmployeeIdNumber().isBlank()) {
            dto.setEmployeeIdNumber(employee.getEmployeeNumber());
        }
        if (dto.getEmployeePhotoUrl() == null || dto.getEmployeePhotoUrl().isBlank()) {
            dto.setEmployeePhotoUrl(employee.getProfilePhotoUrl());
        }
        if (dto.getEmployeeId() == null) {
            dto.setEmployeeId(employee.getId());
        }
        if (dto.getAvailableDays() == null && dto.getMonth() != null && dto.getYear() != null) {
            int availableDays = getDaysInMonth(dto.getMonth(), dto.getYear());
            int paidDays = Math.min(
                    calculatePaidDays(employee.getId(), dto.getMonth(), dto.getYear()), availableDays);
            dto.setAvailableDays(availableDays);
            dto.setPaidDays(paidDays);
            dto.setLossOfPayDays(availableDays - paidDays);
            
            BigDecimal basicSalary = dto.getBasicSalary() != null ? dto.getBasicSalary() : BigDecimal.ZERO;
            BigDecimal dailyRate = basicSalary.divide(BigDecimal.valueOf(availableDays), 4, RoundingMode.HALF_UP);
            
            // Pro-rated earnings based on paid days
            BigDecimal proRatedBasicSalary = dailyRate.multiply(BigDecimal.valueOf(paidDays))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalEarnings = proRatedBasicSalary
                    .add(dto.getBonus() != null ? dto.getBonus() : BigDecimal.ZERO)
                    .add(dto.getCommission() != null ? dto.getCommission() : BigDecimal.ZERO)
                    .add(dto.getOvertime() != null ? dto.getOvertime() : BigDecimal.ZERO);
            
            // Loss of pay amount for display
            BigDecimal lossOfPayAmount = dailyRate.multiply(BigDecimal.valueOf(availableDays - paidDays))
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Deductions = advance only
            BigDecimal advanceDeduction = dto.getAdvanceDeduction() != null
                    ? dto.getAdvanceDeduction()
                    : calculateAdvanceDeduction(employee.getId(), dto.getMonth(), dto.getYear());
            
            dto.setLossOfPayAmount(lossOfPayAmount);
            dto.setAdvanceDeduction(advanceDeduction);
            dto.setTotalDeductions(advanceDeduction);
            dto.setTotalEarnings(totalEarnings);
            dto.setNetSalary(totalEarnings.subtract(advanceDeduction));
            
            // Calculate previous balance for legacy records (latest only, not sum)
            BigDecimal previousBalance = salaryRecordRepository.getLatestPreviousBalanceDueByEmployeeId(employee.getId(), dto.getMonth(), dto.getYear());
            dto.setPreviousBalance(previousBalance != null ? previousBalance : BigDecimal.ZERO);
            
            // Calculate total payable and balance due for legacy records
            BigDecimal totalPayable = dto.getNetSalary().add(dto.getPreviousBalance());
            dto.setTotalPayable(totalPayable);
            
            BigDecimal amountPaid = dto.getAmountPaid() != null ? dto.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal balanceDue = totalPayable.subtract(amountPaid);
            dto.setBalanceDue(balanceDue.max(BigDecimal.ZERO));
        }
        return dto;
    }
    
    @Override
    public SalaryRecordDto markAsPaid(Long salaryId, java.math.BigDecimal amountPaid) {
        SalaryRecord salaryRecord = salaryRecordRepository.findByIdWithLock(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary record not found with id: " + salaryId));

        if ("Paid".equals(salaryRecord.getPaymentStatus())) {
            throw new IllegalArgumentException("Salary is already marked as paid");
        }

        if (salaryRecord.getNetSalary().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cannot process payment for salary with net salary of zero or negative. Net salary: " + salaryRecord.getNetSalary());
        }

        java.math.BigDecimal currentPaid = salaryRecord.getAmountPaid() != null ? salaryRecord.getAmountPaid() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal netSalary = salaryRecord.getNetSalary();
        java.math.BigDecimal paymentToAdd = amountPaid != null ? amountPaid : netSalary;
        java.math.BigDecimal totalPaid = currentPaid.add(paymentToAdd);

        if (totalPaid.compareTo(netSalary) > 0) {
            java.math.BigDecimal remaining = netSalary.subtract(currentPaid);
            throw new IllegalArgumentException(
                "Payment amount exceeds net salary. Net Salary: ₹" + netSalary + ", Already Paid: ₹" + currentPaid + ", Remaining: ₹" + remaining + ", Attempted Payment: ₹" + paymentToAdd
            );
        }

        salaryRecord.setAmountPaid(totalPaid);
        salaryRecord.setPaymentDate(LocalDate.now());

        if (totalPaid.compareTo(netSalary) >= 0) {
            salaryRecord.setPaymentStatus("Paid");
        } else {
            salaryRecord.setPaymentStatus("Partial");
        }

        SalaryRecord updatedSalary = salaryRecordRepository.save(salaryRecord);
        return toDtoEnriched(updatedSalary);
    }
    
    @Override
    public SalaryRecordDto updateBonus(Long salaryId, BigDecimal bonus) {
        // Use pessimistic locking to prevent concurrent bonus updates
        SalaryRecord salaryRecord = salaryRecordRepository.findByIdWithLock(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary record not found with id: " + salaryId));
        
        if ("Paid".equals(salaryRecord.getPaymentStatus())) {
            throw new IllegalArgumentException("Cannot change the bonus of a salary that is already paid");
        }
        if (bonus == null || bonus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bonus must be zero or a positive amount");
        }
        
        salaryRecord.setBonus(bonus);
        calculateSalaryComponents(salaryRecord);
        
        return toDtoEnriched(salaryRecordRepository.save(salaryRecord));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SalaryRecordDto> getSalaryById(Long id) {
        return salaryRecordRepository.findById(id)
                .map(this::toDtoEnriched);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SalaryRecordDto> getSalaryByEmployee(Long employeeId) {
        return salaryRecordRepository.findByEmployeeId(employeeId).stream()
                .map(this::toDtoEnriched)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SalaryRecordDto> getSalaryByMonthAndYear(int month, int year, Long shopId) {
        return salaryRecordRepository.findByShopIdAndMonthAndYear(shopId, month, year).stream()
                .map(this::toDtoEnriched)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SalaryRecordDto> getSalaryByEmployeeAndMonthAndYear(Long employeeId, int month, int year) {
        return salaryRecordRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .map(this::toDtoEnriched);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SalaryRecordDto> getPendingSalaries(Long shopId) {
        return salaryRecordRepository.findPendingByShopId(shopId).stream()
                .map(this::toDtoEnriched)
                .toList();
    }
    
    @Override
    public List<SalaryRecordDto> generateAllSalariesForShop(Long shopId, int month, int year) {
        // Get all active employees for the shop
        List<Employee> employees = employeeRepository.findActiveByShopId(shopId);
        
        List<SalaryRecordDto> generatedSalaries = new java.util.ArrayList<>();
        
        for (Employee employee : employees) {
            try {
                // Reuse the existing record for this month/year so details stay up to date,
                // otherwise start a new one.
                SalaryRecord salaryRecord = salaryRecordRepository
                        .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                        .orElseGet(() -> SalaryRecord.builder()
                                .employee(employee)
                                .month(month)
                                .year(year)
                                .paymentStatus("Pending")
                                .build());
                
                populateSalaryRecord(salaryRecord, employee, month, year);
                
                SalaryRecord savedSalary = salaryRecordRepository.save(salaryRecord);
                generatedSalaries.add(toDtoEnriched(savedSalary));
            } catch (Exception e) {
                logger.error("Failed to generate salary for employee id: {}, name: {}", employee.getId(), employee.getFullName(), e);
            }
        }
        
        return generatedSalaries;
    }
    
    @Override
    public void calculateSalaryComponents(SalaryRecordDto salaryDto) {
        calculateSalaryComponents(salaryRecordMapper.toEntity(salaryDto));
    }
    
    @Override
    public void deleteSalary(Long salaryId) {
        SalaryRecord salaryRecord = salaryRecordRepository.findById(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary record not found with id: " + salaryId));
        salaryRecordRepository.delete(salaryRecord);
    }
    
    private void calculateSalaryComponents(SalaryRecord salaryRecord) {
        BigDecimal basicSalary = salaryRecord.getBasicSalary() != null ? salaryRecord.getBasicSalary() : BigDecimal.ZERO;
        
        // Set allowances to 0 (removed automatic calculation)
        salaryRecord.setHouseAllowance(BigDecimal.ZERO);
        salaryRecord.setTravelAllowance(BigDecimal.ZERO);
        salaryRecord.setMedicalAllowance(BigDecimal.ZERO);
        salaryRecord.setFoodAllowance(BigDecimal.ZERO);
        
        int availableDays = salaryRecord.getAvailableDays() != null && salaryRecord.getAvailableDays() > 0
                ? salaryRecord.getAvailableDays()
                : getDaysInMonth(salaryRecord.getMonth(), salaryRecord.getYear());
        int paidDays = salaryRecord.getPaidDays() != null ? salaryRecord.getPaidDays() : 0;
        int lossOfPayDays = salaryRecord.getLossOfPayDays() != null
                ? salaryRecord.getLossOfPayDays()
                : availableDays - paidDays;
        
        // Earnings = (Basic Salary / Total Days in Month) × Paid Days
        BigDecimal dailyRate = basicSalary.divide(BigDecimal.valueOf(availableDays), 4, RoundingMode.HALF_UP);
        BigDecimal proRatedBasicSalary = dailyRate.multiply(BigDecimal.valueOf(paidDays))
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal totalEarnings = proRatedBasicSalary
                .add(salaryRecord.getBonus() != null ? salaryRecord.getBonus() : BigDecimal.ZERO)
                .add(salaryRecord.getCommission() != null ? salaryRecord.getCommission() : BigDecimal.ZERO)
                .add(salaryRecord.getOvertime() != null ? salaryRecord.getOvertime() : BigDecimal.ZERO);
        
        salaryRecord.setTotalEarnings(totalEarnings);
        
        // Loss of pay = (basic salary / days in month) * unpaid days
        BigDecimal lossOfPayAmount = dailyRate.multiply(BigDecimal.valueOf(lossOfPayDays))
                .setScale(2, RoundingMode.HALF_UP);
        salaryRecord.setLossOfPayAmount(lossOfPayAmount);
        
        // Statutory deductions are not calculated automatically
        salaryRecord.setPf(BigDecimal.ZERO);
        salaryRecord.setEsi(BigDecimal.ZERO);
        salaryRecord.setProfessionalTax(BigDecimal.ZERO);
        salaryRecord.setIncomeTax(BigDecimal.ZERO);
        salaryRecord.setOtherDeductions(BigDecimal.ZERO);
        
        // Deductions = Advance Deduction only (loss of pay is already factored into pro-rated earnings)
        BigDecimal advanceDeduction = salaryRecord.getAdvanceDeduction() != null
                ? salaryRecord.getAdvanceDeduction()
                : BigDecimal.ZERO;
        salaryRecord.setTotalDeductions(advanceDeduction);

        // Net Salary = Total Earnings - Advance Deduction
        // Guard against negative net salary: cap deduction at total earnings
        BigDecimal maxAllowedDeduction = totalEarnings;
        BigDecimal actualDeduction = advanceDeduction.min(maxAllowedDeduction);
        salaryRecord.setTotalDeductions(actualDeduction);
        salaryRecord.setNetSalary(totalEarnings.subtract(actualDeduction));
        
        // Total Payable = Net Salary + Previous Balance
        BigDecimal previousBalance = salaryRecord.getPreviousBalance() != null ? salaryRecord.getPreviousBalance() : BigDecimal.ZERO;
        BigDecimal totalPayable = salaryRecord.getNetSalary().add(previousBalance);
        salaryRecord.setTotalPayable(totalPayable);
        
        // Balance Due = Total Payable - Amount Paid (never negative)
        BigDecimal amountPaid = salaryRecord.getAmountPaid() != null ? salaryRecord.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal balanceDue = totalPayable.subtract(amountPaid);
        salaryRecord.setBalanceDue(balanceDue.max(BigDecimal.ZERO));
    }
    
    private int getDaysInMonth(int month, int year) {
        return java.time.YearMonth.of(year, month).lengthOfMonth();
    }
    
    /**
     * Calculates the advance deduction for a given month using the configured monthly deduction amount,
     * NOT the full advance amount. Rejected advances are ignored.
     * If monthlyDeduction is not set (0 or null), uses the full advance amount for backward compatibility.
     */
    private BigDecimal calculateAdvanceDeduction(Long employeeId, int month, int year) {
        try {
            return advancePaymentRepository.findByEmployeeId(employeeId)
                    .stream()
                    .filter(a -> a.getDate() != null &&
                            a.getDate().getYear() == year &&
                            a.getDate().getMonthValue() == month &&
                            !"Rejected".equals(a.getStatus()))
                    .map(a -> {
                        // Use monthlyDeduction if configured, otherwise fall back to full advance amount
                        BigDecimal monthlyDeduction = a.getMonthlyDeduction();
                        if (monthlyDeduction != null && monthlyDeduction.compareTo(BigDecimal.ZERO) > 0) {
                            return monthlyDeduction;
                        }
                        // Backward compatibility: if no monthly deduction set, use full amount
                        return a.getAdvanceAmount() != null ? a.getAdvanceAmount() : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            logger.error("Error calculating advance deduction for employeeId: {}, month: {}, year: {}", employeeId, month, year, e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Counts the days an employee is paid for in the given month. Present, Late, Half Day,
     * Holiday and Leave are all paid; "Absent" and days with no attendance record are not.
     */
    private int calculatePaidDays(Long employeeId, int month, int year) {
        try {
            return (int) attendanceRepository.findByEmployeeId(employeeId)
                    .stream()
                    .filter(a -> a.getDate() != null &&
                            a.getDate().getYear() == year &&
                            a.getDate().getMonthValue() == month &&
                            PAID_ATTENDANCE_STATUSES.contains(a.getStatus()))
                    .map(a -> a.getDate())
                    .distinct()
                    .count();
        } catch (Exception e) {
            logger.error("Error calculating paid days for employeeId: {}, month: {}, year: {}", employeeId, month, year, e);
            return 0;
        }
    }
}
