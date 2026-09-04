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
        // Use a single GROUP BY query for the last 7 months instead of 7 separate
        // queries each loading ALL attendance records with JOIN FETCH + DTO mapping.
        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusMonths(6).withDayOfMonth(1);

        List<Object[]> rows = attendanceRepository.countByShopIdAndDateRangeGroupByMonthAndStatus(shopId, startDate);

        // Build a lookup map: (year, month) → {status → count}
        Map<String, Map<String, Long>> lookup = new HashMap<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String status = (String) row[2];
            long count = ((Number) row[3]).longValue();
            String key = year + "-" + month;
            lookup.computeIfAbsent(key, k -> new HashMap<>()).put(status, count);
        }

        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = currentDate.minusMonths(i);
            int year = date.getYear();
            int month = date.getMonthValue();
            String key = year + "-" + month;
            Map<String, Long> statusCounts = lookup.getOrDefault(key, java.util.Collections.emptyMap());

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", date.getMonth().name().substring(0, 3));
            monthData.put("present", statusCounts.getOrDefault("Present", 0L));
            monthData.put("absent", statusCounts.getOrDefault("Absent", 0L));
            monthData.put("leave", statusCounts.getOrDefault("Leave", 0L));
            trend.add(monthData);
        }

        return ResponseEntity.ok(trend);
    }
    
    @GetMapping("/shop/{shopId}/salary-distribution")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<Map<String, Object>>> getSalaryDistribution(@PathVariable Long shopId) {
        // Use a single GROUP BY query instead of loading ALL employees and aggregating in Java.
        List<Object[]> rows = employeeRepository.sumBasicSalaryGroupByDepartment(shopId);
        List<Map<String, Object>> distribution = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String department = (String) row[0];
            BigDecimal totalSalary = (BigDecimal) row[1];
            Map<String, Object> data = new HashMap<>();
            data.put("name", department);
            data.put("value", totalSalary != null ? totalSalary : BigDecimal.ZERO);
            distribution.add(data);
        }
        return ResponseEntity.ok(distribution);
    }
    
    @GetMapping("/shop/{shopId}/recent-employees")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<EmployeeDto>> getRecentEmployees(@PathVariable Long shopId) {
        // Use paginated query ordered by joining date desc — loads only 5 employees
        // instead of loading ALL and sorting in Java.
        org.springframework.data.domain.Page<Employee> page = employeeRepository
                .findByShopIdOrderByJoiningDateDesc(shopId,
                        org.springframework.data.domain.PageRequest.of(0, 5));
        return ResponseEntity.ok(page.getContent().stream()
                .map(employeeMapper::toDto)
                .toList());
    }
    
    @GetMapping("/shop/{shopId}/recent-leaves")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<LeaveRequestDto>> getRecentLeaves(@PathVariable Long shopId) {
        // Use paginated query ordered by applied date desc — loads only 5 leave records
        // instead of loading ALL and taking 5 in Java.
        org.springframework.data.domain.Page<swari.sewa.module.employee.entity.LeaveRequest> page =
                leaveRequestRepository.findByShopIdOrderByAppliedDateDesc(shopId,
                        org.springframework.data.domain.PageRequest.of(0, 5));
        return ResponseEntity.ok(page.getContent().stream()
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
                .toList());
    }
    
    @GetMapping("/shop/{shopId}/recent-attendance")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getRecentAttendance(@PathVariable Long shopId) {
        LocalDate today = LocalDate.now();
        List<AttendanceDto> attendance = attendanceService.getAttendanceByDateAndShopId(today, shopId);
        return ResponseEntity.ok(attendance.stream().limit(5).toList());
    }
}
