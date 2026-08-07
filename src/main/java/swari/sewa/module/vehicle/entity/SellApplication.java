package swari.sewa.module.vehicle.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import swari.sewa.common.enums.PaymentMethod;
import swari.sewa.module.vehicle.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sell_applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "vehicle_id", insertable = false, updatable = false)
    private Long vehicleId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_parent_name")
    private String customerParentName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "customer_citizenship_number")
    private String customerCitizenshipNumber;

    @Column(name = "citizenship_front_photo")
    private String citizenshipFrontPhoto;

    @Column(name = "citizenship_back_photo")
    private String citizenshipBackPhoto;

    @Column(name = "customer_photo")
    private String customerPhoto;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "offered_price")
    private BigDecimal offeredPrice;

    @Column(name = "offered_price_in_words")
    private String offeredPriceInWords;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "down_payment")
    private BigDecimal downPayment;

    @Column(name = "financing_required")
    private Boolean financingRequired;

    @Column(name = "financing_bank")
    private String financingBank;

    @Column(name = "sales_man_name")
    private String salesManName;

    @Column(name = "notes")
    private String notes;

    @Column(name = "customer_occupation")
    private String customerOccupation;

    @Column(name = "customer_income")
    private BigDecimal customerIncome;

    @Column(name = "reference_name")
    private String referenceName;

    @Column(name = "reference_phone")
    private String referencePhone;

    @Column(name = "reference_relation")
    private String referenceRelation;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
