package swari.sewa.module.vehicle.dto;

import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.common.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String customerParentName;
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
    private String offeredPriceInWords;
    private PaymentMethod paymentMethod;

    private BigDecimal downPayment;
    private Boolean financingRequired;
    private String financingBank;
    private BigDecimal financingAmount;

    // Sales Information
    private String salesManName;

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
