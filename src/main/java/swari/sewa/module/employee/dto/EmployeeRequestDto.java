package swari.sewa.module.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDto {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    private String employeeNumber;
    
    @NotBlank(message = "Gender is required")
    private String gender;
    
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
    
    private String fatherName;
    
    private String maritalStatus;
    
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;
    
    private String currentAddress;
    
    @Email(message = "Email must be valid")
    private String email;
    
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhar number must be 12 digits")
    private String aadharNumber;
    
    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;
    
    @NotNull(message = "Shop ID is required")
    private Long shopId;
    
    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;
    
    @NotBlank(message = "Designation is required")
    private String designation;
    
    @NotBlank(message = "Employment type is required")
    private String employmentType;
    
    @NotNull(message = "Basic salary is required")
    @Positive(message = "Basic salary must be positive")
    private BigDecimal basicSalary;
    
    private String status = "Active";
    
    private String bankName;
    
    private String accountNumber;
    
    private String ifscCode;
    
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact must be 10 digits")
    private String emergencyContact;
    
    private String emergencyContactName;
    
    private String profilePhotoUrl;
}
