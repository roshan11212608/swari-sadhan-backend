package swari.sewa.module.employee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceRequestDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Present|Absent|Late|Half Day|Holiday|Leave", message = "Status must be Present, Absent, Late, Half Day, Holiday, or Leave")
    private String status;

    private LocalTime clockIn;

    private LocalTime clockOut;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
