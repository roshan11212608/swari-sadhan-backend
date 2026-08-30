package swari.sewa.module.vehicle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.category.entity.Category;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.shop.entity.Shop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "brand_name")
    private String brandName;
    
    @Column(name = "model_name")
    private String modelName;
    
    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;
    
    @Column(name = "registration_number")
    private String registrationNumber;
    
    @Column(name = "lots_number")
    private String lotsNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;
    
    @Column(name = "fuel_type")
    private String fuelType;
    
    @Column(name = "transmission_type")
    private String transmissionType;
    
    @Column(name = "body_type")
    private String bodyType;
    
    @Column(name = "color")
    private String color;
    
    @Column(name = "kilometers_driven")
    private Integer kilometersDriven;
    
    // Convenience getter for mileage (alias for kilometersDriven)
    public Integer getMileage() {
        return kilometersDriven;
    }
    
    // Convenience setter for mileage (alias for kilometersDriven)
    public void setMileage(Integer mileage) {
        this.kilometersDriven = mileage;
    }
    
    // Convenience getter for year (alias for manufacturingYear)
    public Integer getYear() {
        return manufacturingYear;
    }
    
    // Convenience setter for year (alias for manufacturingYear)
    public void setYear(Integer year) {
        this.manufacturingYear = year;
    }
    
    // Convenience getter for type (alias for vehicleType)
    public VehicleType getType() {
        return vehicleType;
    }
    
    // Convenience setter for type (alias for vehicleType)
    public void setType(VehicleType type) {
        this.vehicleType = type;
    }
    
    // Convenience getter for price
    public java.math.BigDecimal getPrice() {
        return price;
    }
    
    // Convenience getter for shop
    public swari.sewa.module.shop.entity.Shop getShop() {
        return shop;
    }
    
    // Convenience getter for mainImageUrl
    public String getMainImageUrl() {
        return mainImageUrl;
    }
    
    // Manual getters for basic fields
    public String getTitle() {
        return title;
    }
    
    // Manual setters for basic fields
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
    
    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
    
    public void setContactCount(long contactCount) {
        this.contactCount = contactCount;
    }
    
    // Additional getters for basic fields
    public Long getContactCount() {
        return contactCount;
    }
    
    public Category getCategory() {
        return category;
    }
    
    // Additional getters for basic fields
    public Long getId() {
        return id;
    }
    
    public VehicleStatus getStatus() {
        return status;
    }
    
    public Long getViewCount() {
        return viewCount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
    }
    
    // Additional getters and setters for basic fields
    public String getCity() {
        return shop != null ? shop.getCity() : null;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    @Column(name = "engine_capacity")
    private String engineCapacity;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;
    
    @Column(name = "is_negotiable")
    @Builder.Default
    private Boolean isNegotiable = true;
    
    @Column(name = "vehicle_condition")
    private String condition;
    
    @Column(name = "ownership_type")
    private String ownershipType;
    
    @Column(name = "insurance_valid")
    private LocalDateTime insuranceValid;
    
    @Column(name = "last_service_date")
    private LocalDateTime lastServiceDate;
    
    @Column(name = "main_image_url")
    private String mainImageUrl;
    
    @ElementCollection
    @CollectionTable(name = "vehicle_images", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "image_url")
    @Builder.Default
    private Set<String> imageUrls = new HashSet<>();
    
    @Column(name = "video_url")
    private String videoUrl;
    
    @Column(name = "seller_passport_photo")
    private String sellerPassportPhoto;
    
    @Column(name = "seller_citizenship_front")
    private String sellerCitizenshipFront;
    
    @Column(name = "seller_citizenship_back")
    private String sellerCitizenshipBack;
    
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;
    
    @Column(name = "features", columnDefinition = "TEXT")
    private String features;
    
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;
    
    @Column(name = "contact_count")
    @Builder.Default
    private Long contactCount = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "sold_at")
    private LocalDateTime soldAt;
    
    @Column(name = "bought_date")
    private LocalDate boughtDate;
    
    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;
    
    @Column(name = "repair_cost", precision = 12, scale = 2)
    private BigDecimal repairCost;
    
    @Column(name = "additional_expenses", precision = 12, scale = 2)
    private BigDecimal additionalExpenses;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Enquiry> enquiries = new HashSet<>();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
