package swari.sewa.module.employee.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import swari.sewa.module.employee.dto.SalaryRecordDto;

public interface SalaryService {
    
    SalaryRecordDto generateSalary(Long employeeId, int month, int year);
    
    SalaryRecordDto markAsPaid(Long salaryId, java.math.BigDecimal amountPaid);
    
    SalaryRecordDto updateBonus(Long salaryId, java.math.BigDecimal bonus);
    
    Optional<SalaryRecordDto> getSalaryById(Long id);
    
    List<SalaryRecordDto> getSalaryByEmployee(Long employeeId);
    
    List<SalaryRecordDto> getSalaryByMonthAndYear(int month, int year, Long shopId);
    
    Optional<SalaryRecordDto> getSalaryByEmployeeAndMonthAndYear(Long employeeId, int month, int year);
    
    List<SalaryRecordDto> getPendingSalaries(Long shopId);
    
    List<SalaryRecordDto> generateAllSalariesForShop(Long shopId, int month, int year);
    
    void calculateSalaryComponents(SalaryRecordDto salaryDto);
    
    void deleteSalary(Long salaryId);
}
