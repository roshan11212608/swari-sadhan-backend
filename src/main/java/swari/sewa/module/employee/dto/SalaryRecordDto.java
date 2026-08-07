package swari.sewa.module.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecordDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeePhotoUrl;
    private String employeeIdNumber;
    private Integer month;
    private Integer year;
    private String monthName;
    private String shopName;
    private String shopLocation;
    private Integer availableDays;
    private Integer paidDays;
    private Integer lossOfPayDays;
    private BigDecimal lossOfPayAmount;
    private BigDecimal basicSalary;
    private BigDecimal houseAllowance;
    private BigDecimal travelAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal foodAllowance;
    private BigDecimal bonus;
    private BigDecimal commission;
    private BigDecimal overtime;
    private BigDecimal totalEarnings;
    private BigDecimal pf;
    private BigDecimal esi;
    private BigDecimal professionalTax;
    private BigDecimal incomeTax;
    private BigDecimal otherDeductions;
    private BigDecimal advanceDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private String paymentStatus;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private BigDecimal previousBalance;
    private BigDecimal totalPayable;
    private LocalDateTime generatedAt;
}
