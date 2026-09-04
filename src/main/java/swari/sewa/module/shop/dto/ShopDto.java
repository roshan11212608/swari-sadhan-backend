package swari.sewa.module.shop.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.ShopStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopDto {
    private Long id;

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    private String postalCode;

    private String phoneNumber;

    private String emailAddress;

    private String websiteUrl;

    private Double latitude;

    private Double longitude;

    private String logoUrl;

    private String openingHours;

    private ShopStatus status;

    private Boolean isFeatured;

    private Integer displayOrder;

    private String subscriptionPlan;

    private LocalDateTime subscriptionExpiry;

    private Long shopOwnerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Frontend-compatible aliases and computed fields
    private String shopName;
    private String shopPhone;
    private String shopEmail;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private String kycStatus;
    private Integer vehicleCount;
    private Integer totalVehicles;
    private Double rating;
    private Integer reviewCount;
    private Integer totalReviews;
    private String logo;
    private String location;
    private String ownerNameDisplay;
}
