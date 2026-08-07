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
public class PublicVehicleListingSellerDto {
    private Long id;
    private String listingNumber;
    private PublicVehicleListingStatus status;
    private String rejectionReason;
    private String reviewNotes;

    private String title;
    private String lotNumber;

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

    // Seller
    private String sellerName;
    private String sellerPhone;
    private String sellerAddress;

    // Owner
    private String ownerName;
    private String ownerPhone;
    private String ownerAddress;

    // Files
    private List<PublicVehicleListingFileDto> files;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime soldAt;
}
