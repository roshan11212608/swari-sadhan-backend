package swari.sewa.module.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.PublicVehicleListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleListingAdminDto {
    private Long id;
    private String listingNumber;
    private String title;
    private String lotNumber;

    // Seller
    private String sellerName;
    private String sellerPhone;
    private String sellerAddress;
    private String sellerEmail;
    private String sellerAccountName;
    private String sellerAccountPhone;

    // Owner
    private String ownerName;
    private String ownerPhone;
    private String ownerAddress;

    // Vehicle
    private String vehicleNumber;
    private String brand;
    private String model;
    private String variant;
    private Integer manufacturingYear;
    private Integer kilometersDriven;
    private String fuelType;
    private String engineCC;
    private String color;

    // Pricing
    private BigDecimal price;
    private String priceInWords;
    private Boolean negotiable;

    // Status & Review
    private PublicVehicleListingStatus status;
    private String adminNotes;
    private String rejectionReason;
    private Boolean declarationAccepted;

    // Files
    private List<PublicVehicleListingFileDto> files;
    private String coverImageUrl;
    private List<String> publicImageUrls;
    private List<String> documentUrls;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime soldAt;
    private LocalDateTime sellerUpdatedAt;
    private List<PublicVehicleListingReviewHistoryDto> reviewHistory;
}
