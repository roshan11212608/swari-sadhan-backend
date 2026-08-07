package swari.sewa.module.employee.dto;

import jakarta.validation.constraints.*;
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
public class EmployeeUpdateDto {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    private String gender;
    
    private LocalDate dateOfBirth;
    
    private String fatherName;
    
    private String maritalStatus;
    
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number")
    private String mobileNumber;
    
    private String currentAddress;
    
    private LocalDate joiningDate;
    
    @NotBlank(message = "Designation is required")
    private String designation;
    
    @NotBlank(message = "Employment type is required")
    private String employmentType;
    
    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.01", message = "Basic salary must be greater than 0")
    @DecimalMax(value = "10000000.00", message = "Basic salary must not exceed 10,000,000")
    private BigDecimal basicSalary;
    
    @Pattern(regexp = "Active|Inactive|OnLeave", message = "Status must be Active, Inactive, or OnLeave")
    private String status;
    
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;
    
    @Pattern(regexp = "^\\d{9,18}$", message = "Bank account number must be between 9 and 18 digits")
    private String accountNumber;
    
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be in valid format (e.g., SBIN0001234)")
    private String ifscCode;
    
    private String emergencyContact;
    
    private String emergencyContactName;
    
    private String profilePhotoUrl;
}
