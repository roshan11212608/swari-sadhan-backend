package swari.sewa.module.employee.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.EmployeeRequestDto;
import swari.sewa.module.employee.dto.EmployeeUpdateDto;

public interface EmployeeService {

    EmployeeDto createEmployee(EmployeeRequestDto requestDto, Long shopId);

    EmployeeDto updateEmployee(Long id, EmployeeUpdateDto employeeDto);

    Optional<EmployeeDto> getEmployeeById(Long id);

    void deleteEmployee(Long id);

    Page<EmployeeDto> getEmployeesByShop(Long shopId, Pageable pageable);

    Page<EmployeeDto> searchEmployees(Long shopId, String search, Pageable pageable);

    Page<EmployeeDto> filterEmployees(Long shopId, String status, String department, String employmentType, Pageable pageable);

    List<EmployeeDto> getAllEmployeesByShop(Long shopId);

    String generateEmployeeNumber(Long shopId);

    // ── Combined search + filter + pagination ──

    Page<EmployeeDto> searchAndFilterEmployees(Long shopId, String search, String status, String department, String employmentType, Pageable pageable);

    // ── Filter options for dropdowns ──

    Map<String, List<String>> getFilterOptions(Long shopId);
}
