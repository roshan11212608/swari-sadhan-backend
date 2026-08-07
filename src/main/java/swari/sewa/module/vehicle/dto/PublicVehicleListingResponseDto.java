package swari.sewa.module.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleListingResponseDto {
    // Core marketplace fields
    private Long id;
    private String listingNumber;
    private String title;
    private String lotNumber;
    private String brand;
    private String model;
    private String variant;
    private Integer year;
    private Integer manufacturingYear;
    private Integer kilometers;
    private Integer kilometersDriven;
    private String fuel;
    private String fuelType;
    private String engineCapacity;
    private String engineCC;
    private String color;
    private String vehicleNumber;
    private BigDecimal price;
    private BigDecimal sellPrice;
    private Boolean negotiable;
    private Boolean exchangeAvailable = false;
    private String status = "Available";

    // Seller / location
    private String sellerName;
    private String sellerPhone;
    private String sellerPhonePrimary;
    private String sellerAddress;
    private String shopName = "Individual Seller";
    private String shopAddress;

    // Images
    private String image;
    private String mainImageUrl;
    private String coverImageUrl;
    private List<String> images;
    private List<String> imageUrls;
    private String videoUrl;

    // Public visibility helpers
    private String listingSource = "Individual Seller";
    private Boolean verified = true;
    private Boolean isPublicListing = true;
    private String source = "public";

    // Timestamps
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
