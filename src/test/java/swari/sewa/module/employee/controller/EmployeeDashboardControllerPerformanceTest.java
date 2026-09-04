package swari.sewa.module.employee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.entity.LeaveRequest;
import swari.sewa.module.employee.mapper.EmployeeMapper;
import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.AttendanceRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.repository.LeaveRequestRepository;
import swari.sewa.module.employee.repository.SalaryRecordRepository;
import swari.sewa.module.employee.service.AttendanceService;
import swari.sewa.module.employee.service.LeaveService;
import swari.sewa.module.employee.service.SalaryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression tests for EmployeeDashboardController performance optimizations.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getRecentEmployees uses paginated query, not findActiveByShopId</li>
 *   <li>getSalaryDistribution uses GROUP BY query, not findActiveByShopId</li>
 *   <li>getAttendanceTrend uses single GROUP BY query, not 7 monthly calls</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmployeeDashboardControllerPerformanceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private SalaryRecordRepository salaryRecordRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private AdvancePaymentRepository advancePaymentRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private SalaryService salaryService;
    @Mock private LeaveService leaveService;
    @Mock private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeDashboardController controller;

    @Test
    void getRecentEmployees_usesPaginatedQuery_notFindActiveByShopId() {
        Long shopId = 1L;
        Employee emp = Employee.builder()
                .id(1L)
                .fullName("Alice")
                .joiningDate(LocalDate.now())
                .build();

        when(employeeRepository.findByShopIdOrderByJoiningDateDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(emp), PageRequest.of(0, 5), 1));
        when(employeeMapper.toDto(any(Employee.class))).thenReturn(
                EmployeeDto.builder().id(1L).fullName("Alice").build());

        var response = controller.getRecentEmployees(shopId);
        List<EmployeeDto> result = response.getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getFullName());

        // Verify paginated query was used
        verify(employeeRepository).findByShopIdOrderByJoiningDateDesc(eq(shopId), any(PageRequest.class));
        // Verify findActiveByShopId was NOT called
        verify(employeeRepository, never()).findActiveByShopId(anyLong());
    }

    @Test
    void getSalaryDistribution_usesGroupByQuery_notFindActiveByShopId() {
        Long shopId = 1L;
        when(employeeRepository.sumBasicSalaryGroupByDepartment(shopId))
                .thenReturn(List.of(
                        new Object[]{"Sales", new BigDecimal("50000")},
                        new Object[]{"Service", new BigDecimal("30000")}
                ));

        var response = controller.getSalaryDistribution(shopId);
        List<Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Sales", result.get(0).get("name"));
        assertEquals(new BigDecimal("50000"), result.get(0).get("value"));
        assertEquals("Service", result.get(1).get("name"));

        // Verify GROUP BY query was used
        verify(employeeRepository).sumBasicSalaryGroupByDepartment(shopId);
        // Verify findActiveByShopId was NOT called
        verify(employeeRepository, never()).findActiveByShopId(anyLong());
    }

    @Test
    void getAttendanceTrend_usesSingleGroupByQuery_not7MonthlyCalls() {
        Long shopId = 1L;
        LocalDate startDate = LocalDate.now().minusMonths(6).withDayOfMonth(1);

        // Simulate GROUP BY results: (year, month, status, count)
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        when(attendanceRepository.countByShopIdAndDateRangeGroupByMonthAndStatus(eq(shopId), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[]{currentYear, currentMonth, "Present", 20L},
                        new Object[]{currentYear, currentMonth, "Absent", 5L},
                        new Object[]{currentYear, currentMonth, "Leave", 3L}
                ));

        var response = controller.getAttendanceTrend(shopId);
        List<Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(7, result.size()); // 7 months

        // Last month should have the data from our mock
        Map<String, Object> lastMonth = result.get(6);
        assertEquals(20L, lastMonth.get("present"));
        assertEquals(5L, lastMonth.get("absent"));
        assertEquals(3L, lastMonth.get("leave"));

        // Verify single GROUP BY query was used
        verify(attendanceRepository).countByShopIdAndDateRangeGroupByMonthAndStatus(eq(shopId), any(LocalDate.class));
        // Verify attendanceService.getAttendanceByMonth was NOT called (old approach)
        verify(attendanceService, never()).getAttendanceByMonth(anyInt(), anyInt(), anyLong());
    }

    @Test
    void getAttendanceTrend_withNoData_returnsZeroCounts() {
        Long shopId = 1L;

        when(attendanceRepository.countByShopIdAndDateRangeGroupByMonthAndStatus(eq(shopId), any(LocalDate.class)))
                .thenReturn(List.of());

        var response = controller.getAttendanceTrend(shopId);
        List<Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(7, result.size());
        // All months should have 0 counts
        for (Map<String, Object> monthData : result) {
            assertEquals(0L, monthData.get("present"));
            assertEquals(0L, monthData.get("absent"));
            assertEquals(0L, monthData.get("leave"));
        }

        verify(attendanceRepository).countByShopIdAndDateRangeGroupByMonthAndStatus(eq(shopId), any(LocalDate.class));
        verify(attendanceService, never()).getAttendanceByMonth(anyInt(), anyInt(), anyLong());
    }

    @Test
    void getRecentLeaves_usesPaginatedQuery_notLoadAll() {
        Long shopId = 1L;
        LeaveRequest leave = LeaveRequest.builder()
                .id(1L)
                .employeeName("Alice")
                .leaveType("Sick Leave")
                .status("Pending")
                .appliedDate(LocalDate.now())
                .build();

        when(leaveRequestRepository.findByShopIdOrderByAppliedDateDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(leave), PageRequest.of(0, 5), 1));

        var response = controller.getRecentLeaves(shopId);
        List<LeaveRequestDto> result = response.getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getEmployeeName());
        assertEquals("Sick Leave", result.get(0).getLeaveType());

        // Verify paginated query was used
        verify(leaveRequestRepository).findByShopIdOrderByAppliedDateDesc(eq(shopId), any(PageRequest.class));
        // Verify full-table load was NOT called (old approach: load ALL + take 5 in Java)
        verify(leaveRequestRepository, never()).findByShopIdOrderByAppliedDateDesc(eq(shopId));
    }
}
