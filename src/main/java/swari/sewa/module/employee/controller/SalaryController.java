package swari.sewa.module.employee.controller;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.SalaryRecordDto;
import swari.sewa.module.employee.service.SalaryService;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SalaryController {
    
    private final SalaryService salaryService;
    
    @PostMapping("/generate")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<SalaryRecordDto> generateSalary(
            @RequestParam Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        SalaryRecordDto salary = salaryService.generateSalary(employeeId, month, year);
        return ResponseEntity.ok(salary);
    }
    
    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isSalaryOwner(#id, authentication.name)")
    public ResponseEntity<SalaryRecordDto> markAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) java.math.BigDecimal amountPaid) {
        SalaryRecordDto salary = salaryService.markAsPaid(id, amountPaid);
        return ResponseEntity.ok(salary);
    }
    
    @PutMapping("/{id}/bonus")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isSalaryOwner(#id, authentication.name)")
    public ResponseEntity<SalaryRecordDto> updateBonus(
            @PathVariable Long id,
            @RequestParam BigDecimal bonus) {
        SalaryRecordDto salary = salaryService.updateBonus(id, bonus);
        return ResponseEntity.ok(salary);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isSalaryOwner(#id, authentication.name)")
    public ResponseEntity<SalaryRecordDto> getSalaryById(@PathVariable Long id) {
        return salaryService.getSalaryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<List<SalaryRecordDto>> getSalaryByEmployee(@PathVariable Long employeeId) {
        List<SalaryRecordDto> salary = salaryService.getSalaryByEmployee(employeeId);
        return ResponseEntity.ok(salary);
    }
    
    @GetMapping("/shop/{shopId}/month/{year}/{month}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<SalaryRecordDto>> getSalaryByMonthAndYear(
            @PathVariable Long shopId,
            @PathVariable int year,
            @PathVariable int month) {
        List<SalaryRecordDto> salary = salaryService.getSalaryByMonthAndYear(month, year, shopId);
        return ResponseEntity.ok(salary);
    }
    
    @GetMapping("/employee/{employeeId}/month/{year}/{month}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<SalaryRecordDto> getSalaryByEmployeeAndMonthAndYear(
            @PathVariable Long employeeId,
            @PathVariable int year,
            @PathVariable int month) {
        return salaryService.getSalaryByEmployeeAndMonthAndYear(employeeId, month, year)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/shop/{shopId}/pending")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<SalaryRecordDto>> getPendingSalaries(@PathVariable Long shopId) {
        List<SalaryRecordDto> salary = salaryService.getPendingSalaries(shopId);
        return ResponseEntity.ok(salary);
    }
    
    @PostMapping("/shop/{shopId}/generate-all")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<SalaryRecordDto>> generateAllSalariesForShop(
            @PathVariable Long shopId,
            @RequestParam int month,
            @RequestParam int year) {
        List<SalaryRecordDto> salaries = salaryService.generateAllSalariesForShop(shopId, month, year);
        return ResponseEntity.ok(salaries);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isSalaryOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteSalary(@PathVariable Long id) {
        salaryService.deleteSalary(id);
        return ResponseEntity.noContent().build();
    }
}
