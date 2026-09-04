package swari.sewa.module.employee.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.employee.dto.AdvancePaymentDto;
import swari.sewa.module.employee.dto.AdvancePaymentRequestDto;
import swari.sewa.module.employee.entity.AdvancePayment;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.mapper.AdvancePaymentMapper;
import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.service.AdvancePaymentService;

@Service
@RequiredArgsConstructor
@Transactional
public class AdvancePaymentServiceImpl implements AdvancePaymentService {
    
    private final AdvancePaymentRepository advancePaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final AdvancePaymentMapper advancePaymentMapper;
    
    @Override
    public AdvancePaymentDto createAdvancePayment(AdvancePaymentRequestDto requestDto) {
        Employee employee = employeeRepository.findById(requestDto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + requestDto.getEmployeeId()));

        // Validate monthly deduction if recovery method is Monthly
        if ("Monthly".equals(requestDto.getRecoveryMethod())) {
            if (requestDto.getMonthlyDeduction() == null || requestDto.getMonthlyDeduction().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Monthly deduction must be greater than 0 when recovery method is Monthly");
            }
            if (requestDto.getMonthlyDeduction().compareTo(requestDto.getAdvanceAmount()) > 0) {
                throw new IllegalArgumentException("Monthly deduction cannot exceed advance amount");
            }
        }

        AdvancePayment advancePayment = advancePaymentMapper.toEntity(requestDto);
        advancePayment.setEmployee(employee);
        advancePayment.setEmployeeName(employee.getFullName());
        advancePayment.setEmployeePhotoUrl(employee.getProfilePhotoUrl());
        advancePayment.setDesignation(employee.getDesignation());
        advancePayment.setDepartment(employee.getDepartment());
        advancePayment.setStatus("Pending");
        advancePayment.setRemainingBalance(requestDto.getAdvanceAmount());
        advancePayment.setRecoveredAmount(BigDecimal.ZERO);

        // Calculate monthly deduction if recovery method is monthly
        if ("Monthly Deduction".equals(requestDto.getRecoveryMethod()) && requestDto.getMonthlyDeduction() != null) {
            advancePayment.setMonthlyDeduction(requestDto.getMonthlyDeduction());
        }
        
        AdvancePayment savedAdvance = advancePaymentRepository.save(advancePayment);
        return advancePaymentMapper.toDto(savedAdvance);
    }
    
    @Override
    public AdvancePaymentDto approveAdvance(Long advanceId, String approvedBy) {
        AdvancePayment advancePayment = advancePaymentRepository.findById(advanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advance payment not found with id: " + advanceId));
        
        if (!"Pending".equals(advancePayment.getStatus())) {
            throw new IllegalArgumentException("Advance payment is not in pending status");
        }
        
        advancePayment.setStatus("Partially Recovered");
        advancePayment.setApprovedBy(approvedBy);
        advancePayment.setApprovedDate(java.time.LocalDate.now());
        
        AdvancePayment updatedAdvance = advancePaymentRepository.save(advancePayment);
        return advancePaymentMapper.toDto(updatedAdvance);
    }
    
    @Override
    public AdvancePaymentDto rejectAdvance(Long advanceId, String rejectionReason) {
        AdvancePayment advancePayment = advancePaymentRepository.findById(advanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advance payment not found with id: " + advanceId));
        
        if (!"Pending".equals(advancePayment.getStatus())) {
            throw new IllegalArgumentException("Advance payment is not in pending status");
        }
        
        advancePayment.setStatus("Rejected");
        advancePayment.setRejectionReason(rejectionReason);
        
        AdvancePayment updatedAdvance = advancePaymentRepository.save(advancePayment);
        return advancePaymentMapper.toDto(updatedAdvance);
    }
    
    @Override
    public AdvancePaymentDto updateRecovery(Long advanceId, BigDecimal recoveredAmount) {
        // Use pessimistic locking to prevent concurrent recovery updates
        AdvancePayment advancePayment = advancePaymentRepository.findByIdWithLock(advanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advance payment not found with id: " + advanceId));
        
        if (!"Partially Recovered".equals(advancePayment.getStatus())) {
            throw new IllegalArgumentException("Advance payment is not in partially recovered status");
        }
        
        BigDecimal newRecoveredAmount = advancePayment.getRecoveredAmount().add(recoveredAmount);
        BigDecimal newRemainingBalance = advancePayment.getAdvanceAmount().subtract(newRecoveredAmount);
        
        if (newRemainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            advancePayment.setStatus("Fully Recovered");
            advancePayment.setRecoveredAmount(advancePayment.getAdvanceAmount());
            advancePayment.setRemainingBalance(BigDecimal.ZERO);
        } else {
            advancePayment.setRecoveredAmount(newRecoveredAmount);
            advancePayment.setRemainingBalance(newRemainingBalance);
        }
        
        AdvancePayment updatedAdvance = advancePaymentRepository.save(advancePayment);
        return advancePaymentMapper.toDto(updatedAdvance);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AdvancePaymentDto> getAdvanceById(Long id) {
        return advancePaymentRepository.findById(id)
                .map(advancePaymentMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AdvancePaymentDto> getAdvanceByEmployee(Long employeeId) {
        return advancePaymentRepository.findByEmployeeId(employeeId).stream()
                .map(advancePaymentMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AdvancePaymentDto> getPendingAdvances(Long shopId) {
        return advancePaymentRepository.findPendingByShopId(shopId).stream()
                .map(advancePaymentMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AdvancePaymentDto> getActiveAdvances(Long shopId) {
        return advancePaymentRepository.findActiveByShopId(shopId).stream()
                .map(advancePaymentMapper::toDto)
                .toList();
    }
    
    @Override
    public void calculateRecovery(AdvancePaymentDto advanceDto) {
        calculateRecovery(advancePaymentMapper.toEntity(advanceDto));
    }
    
    private void calculateRecovery(AdvancePayment advancePayment) {
        BigDecimal advanceAmount = advancePayment.getAdvanceAmount();
        BigDecimal recoveredAmount = advancePayment.getRecoveredAmount() != null ? 
                advancePayment.getRecoveredAmount() : BigDecimal.ZERO;
        
        BigDecimal remainingBalance = advanceAmount.subtract(recoveredAmount);
        
        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            advancePayment.setStatus("Fully Recovered");
            advancePayment.setRemainingBalance(BigDecimal.ZERO);
        } else {
            advancePayment.setRemainingBalance(remainingBalance);
        }
        
        // Calculate monthly deduction if not set
        if (advancePayment.getMonthlyDeduction() == null || advancePayment.getMonthlyDeduction().compareTo(BigDecimal.ZERO) == 0) {
            if ("Monthly Deduction".equals(advancePayment.getRecoveryMethod())) {
                // Default to 6 months for recovery
                BigDecimal monthlyDeduction = advanceAmount.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
                advancePayment.setMonthlyDeduction(monthlyDeduction);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdvanceSummary(Long shopId) {
        Map<String, Object> summary = new HashMap<>();
        BigDecimal totalAdvanceGiven = advancePaymentRepository.sumAdvanceAmountByShopId(shopId);
        BigDecimal recoveredAmount = advancePaymentRepository.sumRecoveredAmountByShopId(shopId);
        BigDecimal pendingRecovery = advancePaymentRepository.sumRemainingBalanceByShopId(shopId);
        Long activeRequests = advancePaymentRepository.countByShopIdAndStatus(shopId, "Pending");

        summary.put("totalAdvanceGiven", totalAdvanceGiven != null ? totalAdvanceGiven : BigDecimal.ZERO);
        summary.put("recoveredAmount", recoveredAmount != null ? recoveredAmount : BigDecimal.ZERO);
        summary.put("pendingRecovery", pendingRecovery != null ? pendingRecovery : BigDecimal.ZERO);
        summary.put("activeRequests", activeRequests != null ? activeRequests : 0L);
        return summary;
    }
}
