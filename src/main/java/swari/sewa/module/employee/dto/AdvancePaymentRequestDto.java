package swari.sewa.module.employee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdvancePaymentRequestDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Advance amount is required")
    @DecimalMin(value = "0.01", message = "Advance amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Advance amount must not exceed 1,000,000")
    private BigDecimal advanceAmount;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @NotNull(message = "Advance date is required")
    @PastOrPresent(message = "Advance date cannot be in the future")
    private LocalDate advanceDate;

    @Pattern(regexp = "Monthly|OneTime", message = "Recovery method must be Monthly or OneTime")
    private String recoveryMethod;

    @DecimalMin(value = "0.01", message = "Monthly deduction must be greater than 0")
    private BigDecimal monthlyDeduction;
}
