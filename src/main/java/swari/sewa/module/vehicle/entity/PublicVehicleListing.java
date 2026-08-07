package swari.sewa.module.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import swari.sewa.common.enums.PublicVehicleListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "public_vehicle_listings",
    indexes = {
        @Index(name = "idx_pvl_status", columnList = "status"),
        @Index(name = "idx_pvl_listing_number", columnList = "listing_number", unique = true),
        @Index(name = "idx_pvl_seller_user", columnList = "seller_user_id"),
        @Index(name = "idx_pvl_vehicle_number", columnList = "vehicle_number"),
        @Index(name = "idx_pvl_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicVehicleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_number", nullable = false, unique = true)
    private String listingNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "seller_user_id")
    private Long sellerUserId;

    // Seller Information
    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "seller_phone", nullable = false)
    private String sellerPhone;

    @Column(name = "seller_address", columnDefinition = "TEXT")
    private String sellerAddress;

    // Owner Information
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owner_phone", nullable = false)
    private String ownerPhone;

    @Column(name = "owner_address", columnDefinition = "TEXT")
    private String ownerAddress;

    // Vehicle Information
    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "variant")
    private String variant;

    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @Column(name = "kilometers_driven", nullable = false)
    private Integer kilometersDriven;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "engine_cc")
    private String engineCC;

    @Column(name = "color", nullable = false)
    private String color;

    // Pricing
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "price_in_words", columnDefinition = "TEXT")
    private String priceInWords;

    @Column(name = "negotiable")
    private Boolean negotiable = false;

    // Admin / Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PublicVehicleListingStatus status = PublicVehicleListingStatus.DRAFT;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "declaration_accepted", nullable = false)
    private Boolean declarationAccepted = false;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "seller_updated_at")
    private LocalDateTime sellerUpdatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PublicVehicleListingFile> files = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == PublicVehicleListingStatus.DRAFT && submittedAt == null) {
            submittedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addFile(PublicVehicleListingFile file) {
        files.add(file);
        file.setListing(this);
    }

    public void removeFile(PublicVehicleListingFile file) {
        files.remove(file);
        file.setListing(null);
    }
}
