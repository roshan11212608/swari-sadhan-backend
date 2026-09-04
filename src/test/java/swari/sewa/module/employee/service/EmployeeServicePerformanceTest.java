package swari.sewa.module.employee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.mapper.EmployeeMapper;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.service.impl.EmployeeServiceImpl;
import swari.sewa.module.shop.repository.ShopRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression tests for Employee Management performance optimization.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>searchAndFilterEmployees uses the combined repository query (not /all)</li>
 *   <li>getFilterOptions calls the distinct-value repository methods</li>
 *   <li>Null/empty parameters are normalized correctly</li>
 *   <li>The /all method is NOT called by the new paginated path</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServicePerformanceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee createTestEmployee(Long id, String name, String dept, String status, String empType) {
        return Employee.builder()
                .id(id)
                .employeeNumber("EMP1-" + String.format("%03d", id))
                .fullName(name)
                .gender("Male")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .mobileNumber("9999999999")
                .joiningDate(LocalDate.now().minusDays(id))
                .department(dept)
                .designation("Worker")
                .employmentType(empType)
                .basicSalary(BigDecimal.valueOf(10000))
                .status(status)
                .build();
    }

    @Test
    void searchAndFilterEmployees_usesCombinedQuery_notFindAll() {
        Long shopId = 1L;
        List<Employee> employees = List.of(
                createTestEmployee(1L, "Alice", "Sales", "Active", "Full-time"),
                createTestEmployee(2L, "Bob", "Sales", "Active", "Full-time")
        );

        when(employeeRepository.findByShopIdWithSearchAndFilters(
                eq(shopId), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(employees, PageRequest.of(0, 10), 2));

        when(employeeMapper.toDto(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            return EmployeeDto.builder()
                    .id(e.getId())
                    .fullName(e.getFullName())
                    .department(e.getDepartment())
                    .status(e.getStatus())
                    .employmentType(e.getEmploymentType())
                    .build();
        });

        Page<EmployeeDto> result = employeeService.searchAndFilterEmployees(
                shopId, null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());

        // Verify combined query was used
        verify(employeeRepository).findByShopIdWithSearchAndFilters(
                eq(shopId), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class));

        // Verify /all method was NOT called
        verify(employeeRepository, never()).findActiveByShopId(anyLong());
    }

    @Test
    void searchAndFilterEmployees_withSearchAndFilters_appliesAllParams() {
        Long shopId = 1L;
        List<Employee> employees = List.of(
                createTestEmployee(1L, "Alice Smith", "Sales", "Active", "Full-time")
        );

        when(employeeRepository.findByShopIdWithSearchAndFilters(
                eq(shopId), eq("alice"), eq("Active"), eq("Sales"), eq("Full-time"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(employees, PageRequest.of(0, 10), 1));

        when(employeeMapper.toDto(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            return EmployeeDto.builder()
                    .id(e.getId())
                    .fullName(e.getFullName())
                    .build();
        });

        Page<EmployeeDto> result = employeeService.searchAndFilterEmployees(
                shopId, "alice", "Active", "Sales", "Full-time", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Alice Smith", result.getContent().get(0).getFullName());

        verify(employeeRepository).findByShopIdWithSearchAndFilters(
                eq(shopId), eq("alice"), eq("Active"), eq("Sales"), eq("Full-time"), any(PageRequest.class));
    }

    @Test
    void searchAndFilterEmployees_normalizesEmptyStringsToNull() {
        Long shopId = 1L;

        when(employeeRepository.findByShopIdWithSearchAndFilters(
                eq(shopId), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // Pass empty strings — should be normalized to null
        employeeService.searchAndFilterEmployees(
                shopId, "  ", "", "  ", null, PageRequest.of(0, 10));

        verify(employeeRepository).findByShopIdWithSearchAndFilters(
                eq(shopId), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class));
    }

    @Test
    void getFilterOptions_callsDistinctRepositoryMethods() {
        Long shopId = 1L;

        when(employeeRepository.findDistinctDepartmentsByShopId(shopId))
                .thenReturn(List.of("Sales", "Service", "Admin"));
        when(employeeRepository.findDistinctEmploymentTypesByShopId(shopId))
                .thenReturn(List.of("Full-time", "Part-time"));
        when(employeeRepository.findDistinctStatusesByShopId(shopId))
                .thenReturn(List.of("Active", "Inactive"));

        Map<String, List<String>> result = employeeService.getFilterOptions(shopId);

        assertNotNull(result);
        assertEquals(3, result.get("departments").size());
        assertEquals(2, result.get("employmentTypes").size());
        assertEquals(2, result.get("statuses").size());
        assertTrue(result.get("departments").contains("Sales"));
        assertTrue(result.get("employmentTypes").contains("Full-time"));
        assertTrue(result.get("statuses").contains("Active"));

        // Verify distinct-value queries were used, not findActiveByShopId
        verify(employeeRepository).findDistinctDepartmentsByShopId(shopId);
        verify(employeeRepository).findDistinctEmploymentTypesByShopId(shopId);
        verify(employeeRepository).findDistinctStatusesByShopId(shopId);
        verify(employeeRepository, never()).findActiveByShopId(anyLong());
    }

    @Test
    void getFilterOptions_withNoData_returnsEmptyLists() {
        Long shopId = 999L;

        when(employeeRepository.findDistinctDepartmentsByShopId(shopId)).thenReturn(List.of());
        when(employeeRepository.findDistinctEmploymentTypesByShopId(shopId)).thenReturn(List.of());
        when(employeeRepository.findDistinctStatusesByShopId(shopId)).thenReturn(List.of());

        Map<String, List<String>> result = employeeService.getFilterOptions(shopId);

        assertNotNull(result);
        assertTrue(result.get("departments").isEmpty());
        assertTrue(result.get("employmentTypes").isEmpty());
        assertTrue(result.get("statuses").isEmpty());
    }
}
