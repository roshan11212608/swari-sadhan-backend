package swari.sewa.module.employee.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.EmployeeRequestDto;
import swari.sewa.module.employee.dto.EmployeeUpdateDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.mapper.EmployeeMapper;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.service.EmployeeService;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    private final ShopRepository shopRepository;
    private final EmployeeMapper employeeMapper;
    
    @Override
    public EmployeeDto createEmployee(EmployeeRequestDto requestDto, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        // Check if employee number already exists
        if (requestDto.getEmployeeNumber() != null && !requestDto.getEmployeeNumber().trim().isEmpty()) {
            if (employeeRepository.findByEmployeeNumber(requestDto.getEmployeeNumber()).isPresent()) {
                throw new IllegalArgumentException("Employee with this employee number already exists");
            }
        }

        // Check for duplicate mobile number within the same shop
        if (requestDto.getMobileNumber() != null) {
            List<Employee> existingByMobile = employeeRepository.findByMobileNumber(requestDto.getMobileNumber());
            if (existingByMobile.stream().anyMatch(e -> e.getShop().getId().equals(shopId))) {
                throw new IllegalArgumentException("An employee with this mobile number already exists in this shop");
            }
        }

        Employee employee = employeeMapper.toEntity(requestDto);
        employee.setShop(shop);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(savedEmployee);
    }
    
    @Override
    public EmployeeDto updateEmployee(Long id, EmployeeUpdateDto employeeDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Check for duplicate mobile number within the same shop
        if (employeeDto.getMobileNumber() != null && !employeeDto.getMobileNumber().equals(employee.getMobileNumber())) {
            List<Employee> existingByMobile = employeeRepository.findByMobileNumber(employeeDto.getMobileNumber());
            boolean duplicate = existingByMobile.stream()
                .anyMatch(e -> !e.getId().equals(id) && e.getShop().getId().equals(employee.getShop().getId()));
            if (duplicate) {
                throw new IllegalArgumentException("An employee with this mobile number already exists in this shop");
            }
        }

        employeeMapper.updateEntityFromUpdateDto(employeeDto, employee);
        employee.setUpdatedAt(LocalDateTime.now());

        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(updatedEmployee);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeDto> getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toDto);
    }
    
    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        // Cascade soft delete to related records
        LocalDateTime now = LocalDateTime.now();
        
        employee.getAttendanceRecords().forEach(attendance -> attendance.setDeletedAt(now));
        employee.getSalaryRecords().forEach(salary -> salary.setDeletedAt(now));
        employee.getLeaveRequests().forEach(leave -> leave.setDeletedAt(now));
        employee.getAdvancePayments().forEach(advance -> advance.setDeletedAt(now));
        
        // Soft delete employee
        employee.setDeletedAt(now);
        employeeRepository.save(employee);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> getEmployeesByShop(Long shopId, Pageable pageable) {
        return employeeRepository.findByShopId(shopId, pageable)
                .map(employeeMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> searchEmployees(Long shopId, String search, Pageable pageable) {
        return employeeRepository.searchEmployees(shopId, search, pageable)
                .map(employeeMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> filterEmployees(Long shopId, String status, String department, String employmentType, Pageable pageable) {
        Page<Employee> employees;
        
        if (status != null && department != null && employmentType != null) {
            // All filters - use combined query
            employees = employeeRepository.findByShopIdAndStatusAndDepartmentAndEmploymentType(shopId, status, department, employmentType, pageable);
        } else if (status != null && department != null) {
            // Status + Department
            employees = employeeRepository.findByShopIdAndStatusAndDepartment(shopId, status, department, pageable);
        } else if (status != null && employmentType != null) {
            // Status + Employment Type
            employees = employeeRepository.findByShopIdAndStatusAndEmploymentType(shopId, status, employmentType, pageable);
        } else if (department != null && employmentType != null) {
            // Department + Employment Type
            employees = employeeRepository.findByShopIdAndDepartmentAndEmploymentType(shopId, department, employmentType, pageable);
        } else if (status != null) {
            employees = employeeRepository.findByShopIdAndStatus(shopId, status, pageable);
        } else if (department != null) {
            employees = employeeRepository.findByShopIdAndDepartment(shopId, department, pageable);
        } else if (employmentType != null) {
            employees = employeeRepository.findByShopIdAndEmploymentType(shopId, employmentType, pageable);
        } else {
            employees = employeeRepository.findByShopId(shopId, pageable);
        }
        
        return employees.map(employeeMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployeesByShop(Long shopId) {
        return employeeRepository.findActiveByShopId(shopId).stream()
                .map(employeeMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String generateEmployeeNumber(Long shopId) {
        // Find the highest existing employee number for the given shop
        Optional<String> latestEmployeeNumber = employeeRepository.findTopByShopIdOrderByEmployeeNumberDesc(shopId)
                .map(Employee::getEmployeeNumber);
        
        long nextNumber = 1;
        if (latestEmployeeNumber.isPresent()) {
            String[] parts = latestEmployeeNumber.get().split("-");
            if (parts.length == 2 && parts[0].startsWith("EMP")) {
                try {
                    nextNumber = Long.parseLong(parts[1]) + 1;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse employee number '{}', falling back to 1", latestEmployeeNumber.get(), e);
                    nextNumber = 1;
                }
            }
        }
        
        return String.format("EMP%d-%03d", shopId, nextNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> searchAndFilterEmployees(Long shopId, String search, String status, String department, String employmentType, Pageable pageable) {
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String normalizedStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String normalizedDepartment = (department != null && !department.trim().isEmpty()) ? department.trim() : null;
        String normalizedEmploymentType = (employmentType != null && !employmentType.trim().isEmpty()) ? employmentType.trim() : null;

        return employeeRepository.findByShopIdWithSearchAndFilters(
                shopId, normalizedSearch, normalizedStatus, normalizedDepartment, normalizedEmploymentType, pageable)
                .map(employeeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getFilterOptions(Long shopId) {
        Map<String, List<String>> options = new HashMap<>();
        options.put("departments", employeeRepository.findDistinctDepartmentsByShopId(shopId));
        options.put("employmentTypes", employeeRepository.findDistinctEmploymentTypesByShopId(shopId));
        options.put("statuses", employeeRepository.findDistinctStatusesByShopId(shopId));
        return options;
    }
}
