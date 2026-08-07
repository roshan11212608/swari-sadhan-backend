package swari.sewa.module.employee.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.EmployeeRequestDto;
import swari.sewa.module.employee.dto.EmployeeUpdateDto;
import swari.sewa.module.employee.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeController {
    
    private final EmployeeService employeeService;
    
    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#requestDto.shopId, authentication.name)")
    public ResponseEntity<EmployeeDto> createEmployee(
            @Valid @RequestBody EmployeeRequestDto requestDto) {
        EmployeeDto employee = employeeService.createEmployee(requestDto, requestDto.getShopId());
        return ResponseEntity.ok(employee);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#id, authentication.name)")
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateDto employeeDto) {
        EmployeeDto updatedEmployee = employeeService.updateEmployee(id, employeeDto);
        return ResponseEntity.ok(updatedEmployee);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#id, authentication.name)")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<Page<EmployeeDto>> getEmployeesByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EmployeeDto> employees = employeeService.getEmployeesByShop(shopId, 
                org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/shop/{shopId}/all")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<EmployeeDto>> getAllEmployeesByShop(@PathVariable Long shopId) {
        List<EmployeeDto> employees = employeeService.getAllEmployeesByShop(shopId);
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/shop/{shopId}/search")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<Page<EmployeeDto>> searchEmployees(
            @PathVariable Long shopId,
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EmployeeDto> employees = employeeService.searchEmployees(shopId, search,
                org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/shop/{shopId}/filter")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<Page<EmployeeDto>> filterEmployees(
            @PathVariable Long shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EmployeeDto> employees = employeeService.filterEmployees(shopId, status, department, employmentType,
                org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(employees);
    }
}
