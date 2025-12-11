package swari.sewa.module.publicuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.EnquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryDto {
    private Long id;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;
    
    private String customerPhone;
    
    private String message;
    
    private String preferredContactMethod;
    
    private String budgetRange;
    
    private String expectedPurchaseTime;
    
    private Boolean financingRequired;
    
    private Boolean testDriveRequested;
    
    private EnquiryStatus status;
    
    private String adminNotes;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Long customerId;
    
    private Long vehicleId;
    
    private Long shopId;
    
    private String vehicleTitle;
    
    private String shopName;
}
