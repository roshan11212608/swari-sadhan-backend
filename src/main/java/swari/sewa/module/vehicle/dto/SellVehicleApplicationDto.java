package swari.sewa.module.vehicle.dto;

import lombok.*;
import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellVehicleApplicationDto {

    private Long id;
    private Long vehicleId;
    private Long shopId;
    private String vehicleTitle;
    private String vehicleBrand;
    private String vehicleModel;
    private BigDecimal vehiclePrice;

    // Customer Information
    private String customerName;
    private String customerParentName; // Added for parent name
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String customerCitizenshipNumber;
    
    // Customer Photos/Documents
    private String customerPhoto;
    private String citizenshipFrontPhoto;
    private String citizenshipBackPhoto;

    // Application Details
    private LocalDateTime applicationDate;
    private BigDecimal offeredPrice;
    private String offeredPriceInWords; // Added for price in words
    private PaymentMethod paymentMethod;
    
    // Custom setter to handle string conversion from FormData
    public void setPaymentMethod(Object paymentMethod) {
        if (paymentMethod instanceof String) {
            this.paymentMethod = PaymentMethod.valueOf(((String) paymentMethod).toUpperCase());
        } else if (paymentMethod instanceof PaymentMethod) {
            this.paymentMethod = (PaymentMethod) paymentMethod;
        } else {
            this.paymentMethod = PaymentMethod.CASH; // Default fallback
        }
    }
    private BigDecimal downPayment;
    private Boolean financingRequired;
    private String financingBank;
    private BigDecimal financingAmount;
    
    // Sales Information
    private String salesManName; // Added for sales person information

    // Additional Information
    private String customerOccupation;
    private BigDecimal customerIncome;
    private String referenceName;
    private String referencePhone;
    private String referenceRelation;

    // Documents
    private Boolean citizenshipCopyProvided;
    private Boolean photoProvided;
    private Boolean addressProofProvided;
    private Boolean incomeProofProvided;

    // Terms and Conditions
    private Boolean termsAccepted;
    private Boolean backgroundCheckConsent;

    // Application Status
    private ApplicationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private String notes;
}
