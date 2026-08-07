package swari.sewa.module.employee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestRequestDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Leave type is required")
    @Pattern(regexp = "Sick Leave|Casual Leave|Earned Leave|Maternity Leave|Paternity Leave|Other", message = "Leave type must be a valid leave type")
    private String leaveType;

    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
    
    // Custom validation for endDate >= startDate will be handled in service layer
}
