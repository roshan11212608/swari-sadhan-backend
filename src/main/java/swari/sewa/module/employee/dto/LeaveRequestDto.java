package swari.sewa.module.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeePhotoUrl;
    private String designation;
    private String department;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private String reason;
    private String status;
    private LocalDate appliedDate;
    private String approvedBy;
    private LocalDate approvedDate;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
