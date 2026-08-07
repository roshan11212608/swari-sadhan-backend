package swari.sewa.module.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate date;
    private String status;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private BigDecimal workingHours;
    private BigDecimal overtime;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
