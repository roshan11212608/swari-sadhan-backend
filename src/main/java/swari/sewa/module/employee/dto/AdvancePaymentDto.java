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
public class AdvancePaymentDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeePhotoUrl;
    private String designation;
    private String department;
    private BigDecimal advanceAmount;
    private String reason;
    private LocalDate date;
    private String recoveryMethod;
    private BigDecimal monthlyDeduction;
    private BigDecimal recoveredAmount;
    private BigDecimal remainingBalance;
    private String status;
    private String approvedBy;
    private LocalDate approvedDate;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
