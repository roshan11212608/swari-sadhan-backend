package swari.sewa.module.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.common.enums.PaymentMethod;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.vehicle.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sell_vehicle_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellVehicleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // Customer Information
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_parent_name")
    private String customerParentName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_address", nullable = false, columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "customer_citizenship_number", nullable = false)
    private String customerCitizenshipNumber;

    // Customer Photos/Documents
    @Column(name = "customer_photo", columnDefinition = "TEXT")
    private String customerPhoto;

    @Column(name = "citizenship_front_photo", columnDefinition = "TEXT")
    private String citizenshipFrontPhoto;

    @Column(name = "citizenship_back_photo", columnDefinition = "TEXT")
    private String citizenshipBackPhoto;

    // Application Details
    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "offered_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal offeredPrice;

    @Column(name = "offered_price_in_words", columnDefinition = "TEXT")
    private String offeredPriceInWords;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "down_payment", precision = 19, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "financing_required")
    private Boolean financingRequired = false;

    @Column(name = "financing_bank")
    private String financingBank;

    @Column(name = "financing_amount", precision = 19, scale = 2)
    private BigDecimal financingAmount;

    // Sales Information
    @Column(name = "sales_man_name")
    private String salesManName;

    // Additional Information
    @Column(name = "customer_occupation")
    private String customerOccupation;

    @Column(name = "customer_income", precision = 19, scale = 2)
    private BigDecimal customerIncome;

    @Column(name = "reference_name")
    private String referenceName;

    @Column(name = "reference_phone")
    private String referencePhone;

    @Column(name = "reference_relation")
    private String referenceRelation;

    // Documents
    @Column(name = "citizenship_copy_provided")
    private Boolean citizenshipCopyProvided = false;

    @Column(name = "photo_provided")
    private Boolean photoProvided = false;

    @Column(name = "address_proof_provided")
    private Boolean addressProofProvided = false;

    @Column(name = "income_proof_provided")
    private Boolean incomeProofProvided = false;

    // Terms and Conditions
    @Column(name = "terms_accepted")
    private Boolean termsAccepted = false;

    @Column(name = "background_check_consent")
    private Boolean backgroundCheckConsent = false;

    // Application Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
