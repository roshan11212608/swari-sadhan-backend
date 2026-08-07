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
public class EmployeeDto {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String fatherName;
    private String maritalStatus;
    private String mobileNumber;
    private String currentAddress;
    private LocalDate joiningDate;
    private String department;
    private String designation;
    private String employmentType;
    private BigDecimal basicSalary;
    private String status;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String emergencyContact;
    private String emergencyContactName;
    private String profilePhotoUrl;
    private Long shopId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
