package swari.sewa.module.employee.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.AttendanceDto;
import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.dto.SalaryRecordDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.mapper.EmployeeMapper;
import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.AttendanceRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.repository.LeaveRequestRepository;
import swari.sewa.module.employee.repository.SalaryRecordRepository;
import swari.sewa.module.employee.service.AttendanceService;
import swari.sewa.module.employee.service.LeaveService;
import swari.sewa.module.employee.service.SalaryService;

@RestController
@RequestMapping("/api/employee-dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeDashboardController {
    
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AdvancePaymentRepository advancePaymentRepository;
    private final AttendanceService attendanceService;
    private final SalaryService salaryService;
    private final LeaveService leaveService;
    private final EmployeeMapper employeeMapper;
    
    @GetMapping("/shop/{shopId}/kpi")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<Map<String, Object>> getDashboardKPIs(
            @PathVariable Long shopId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();
        
        Map<String, Object> kpis = new HashMap<>();
        
        // Employee counts
        Long totalEmployees = employeeRepository.countActiveByShopId(shopId);
        Long activeEmployees = employeeRepository.countByShopIdAndStatus(shopId, "Active");
        kpis.put("totalEmployees", totalEmployees);
        kpis.put("activeEmployees", activeEmployees);
        
        // Today's attendance
        LocalDate today = LocalDate.now();
        Long presentToday = attendanceRepository.countByShopIdAndDateAndStatus(shopId, today, "Present");
        Long onLeaveToday = attendanceRepository.countByShopIdAndDateAndStatus(shopId, today, "Leave");
        kpis.put("presentToday", presentToday);
        kpis.put("onLeaveToday", onLeaveToday);
        
        // Salary for the month
        BigDecimal totalMonthlySalary = salaryRecordRepository.sumNetSalaryByShopIdAndMonthAndYear(shopId, month, year);
        BigDecimal salaryPaid = salaryRecordRepository.sumPaidSalaryByShopIdAndMonthAndYear(shopId, month, year);
        BigDecimal pendingSalary = totalMonthlySalary != null && salaryPaid != null ? 
                totalMonthlySalary.subtract(salaryPaid) : (totalMonthlySalary != null ? totalMonthlySalary : BigDecimal.ZERO);
        
        kpis.put("totalMonthlySalary", totalMonthlySalary != null ? totalMonthlySalary : BigDecimal.ZERO);
        kpis.put("salaryPaid", salaryPaid != null ? salaryPaid : BigDecimal.ZERO);
        kpis.put("pendingSalary", pendingSalary);
        
        // Advance payments
        BigDecimal totalAdvances = advancePaymentRepository.sumAdvanceAmountByShopId(shopId);
        BigDecimal totalAdvancesRecovered = advancePaymentRepository.sumRecoveredAmountByShopId(shopId);
        BigDecimal totalAdvancesPending = advancePaymentRepository.sumRemainingBalanceByShopId(shopId);
        
        kpis.put("totalAdvances", totalAdvances != null ? totalAdvances : BigDecimal.ZERO);
        kpis.put("totalAdvancesRecovered", totalAdvancesRecovered != null ? totalAdvancesRecovered : BigDecimal.ZERO);
        kpis.put("totalAdvancesPending", totalAdvancesPending != null ? totalAdvancesPending : BigDecimal.ZERO);
        
        return ResponseEntity.ok(kpis);
    }
    
    @GetMapping("/shop/{shopId}/attendance-trend")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceTrend(@PathVariable Long shopId) {
        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        
        // Get last 7 months attendance trend
        LocalDate currentDate = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = currentDate.minusMonths(i);
            int year = date.getYear();
            int month = date.getMonthValue();
            
            List<AttendanceDto> attendance = attendanceService.getAttendanceByMonth(year, month, shopId);
            
            long present = attendance.stream().filter(a -> "Present".equals(a.getStatus())).count();
            long absent = attendance.stream().filter(a -> "Absent".equals(a.getStatus())).count();
            long leave = attendance.stream().filter(a -> "Leave".equals(a.getStatus())).count();
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", date.getMonth().name().substring(0, 3));
            monthData.put("present", present);
            monthData.put("absent", absent);
            monthData.put("leave", leave);
            
            trend.add(monthData);
        }
        
        return ResponseEntity.ok(trend);
    }
    
    @GetMapping("/shop/{shopId}/salary-distribution")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<Map<String, Object>>> getSalaryDistribution(@PathVariable Long shopId) {
        List<Employee> employees = employeeRepository.findActiveByShopId(shopId);
        
        Map<String, BigDecimal> departmentSalary = new HashMap<>();
        for (Employee employee : employees) {
            String department = employee.getDepartment();
            BigDecimal salary = employee.getBasicSalary();
            departmentSalary.put(department, departmentSalary.getOrDefault(department, BigDecimal.ZERO).add(salary));
        }
        
        List<Map<String, Object>> distribution = new java.util.ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : departmentSalary.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", entry.getKey());
            data.put("value", entry.getValue());
            distribution.add(data);
        }
        
        return ResponseEntity.ok(distribution);
    }
    
    @GetMapping("/shop/{shopId}/recent-employees")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<EmployeeDto>> getRecentEmployees(@PathVariable Long shopId) {
        List<Employee> employees = employeeRepository.findActiveByShopId(shopId);
        // Sort by joining date descending and take first 5
        return ResponseEntity.ok(employees.stream()
                .sorted((e1, e2) -> e2.getJoiningDate().compareTo(e1.getJoiningDate()))
                .limit(5)
                .map(employeeMapper::toDto)
                .toList());
    }
    
    @GetMapping("/shop/{shopId}/recent-leaves")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<LeaveRequestDto>> getRecentLeaves(@PathVariable Long shopId) {
        List<LeaveRequestDto> leaves = leaveRequestRepository.findByShopIdOrderByAppliedDateDesc(shopId).stream()
                .map(leave -> {
                    LeaveRequestDto dto = new LeaveRequestDto();
                    dto.setId(leave.getId());
                    dto.setEmployeeId(leave.getEmployee() != null ? leave.getEmployee().getId() : null);
                    dto.setEmployeeName(leave.getEmployeeName());
                    dto.setEmployeePhotoUrl(leave.getEmployeePhotoUrl());
                    dto.setDesignation(leave.getDesignation());
                    dto.setDepartment(leave.getDepartment());
                    dto.setLeaveType(leave.getLeaveType());
                    dto.setStartDate(leave.getStartDate());
                    dto.setEndDate(leave.getEndDate());
                    dto.setDuration(leave.getDuration());
                    dto.setReason(leave.getReason());
                    dto.setStatus(leave.getStatus());
                    dto.setAppliedDate(leave.getAppliedDate());
                    dto.setApprovedBy(leave.getApprovedBy());
                    dto.setApprovedDate(leave.getApprovedDate());
                    dto.setRejectionReason(leave.getRejectionReason());
                    return dto;
                })
                .limit(5)
                .toList();
        return ResponseEntity.ok(leaves);
    }
    
    @GetMapping("/shop/{shopId}/recent-attendance")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getRecentAttendance(@PathVariable Long shopId) {
        LocalDate today = LocalDate.now();
        List<AttendanceDto> attendance = attendanceService.getAttendanceByDateAndShopId(today, shopId);
        return ResponseEntity.ok(attendance.stream().limit(5).toList());
    }
}
