package swari.sewa.module.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {
    private Long id;
    
    @NotBlank(message = "Vehicle title is required")
    private String title;
    
    private String description;
    
    private String brandName;
    
    private String modelName;
    
    private Integer manufacturingYear;
    
    private String registrationNumber;
    
    private String lotsNumber;
    
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    
    private String fuelType;
    
    private String transmissionType;
    
    private String bodyType;
    
    private String color;
    
    private Integer kilometersDriven;
    
    @JsonAlias({"engineCC", "engineCc"})
    private String engineCapacity;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal sellPrice;

    private BigDecimal purchasePrice;

    private BigDecimal additionalExpenditure;

    private Boolean isNegotiable;
    
    private String condition;
    
    private String ownershipType;
    
    private LocalDateTime insuranceValid;
    
    private LocalDateTime lastServiceDate;
    
    private String mainImageUrl;
    
    private Set<String> imageUrls;
    
    private String videoUrl;
    
    private String sellerPassportPhoto;
    
    private String sellerCitizenshipFront;
    
    private String sellerCitizenshipBack;
    
    private String specifications;
    
    private String features;
    
    private Long viewCount;
    
    private Long contactCount;
    
    private VehicleStatus status;
    
    private String rejectionReason;
    
    private Boolean isFeatured;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime soldAt;
    
    private LocalDate boughtDate;
    
    private Long shopId;

    private Long categoryId;

    private String shopName;

    private String shopCity;

    private String shopPhone;

    private String shopEmail;

    private String shopAddress;

    // Shop owner address details
    private String shopProvince;

    private String shopDistrict;

    private String shopMunicipality;

    private String shopWard;

    private String shopTole;

    private String categoryName;
    
    // Manual getters for basic fields
    public Long getCategoryId() {
        return categoryId;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    // Manual setter for shopName
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    // Manual setters for category fields
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    // Manual setter for shopId
    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }
}
