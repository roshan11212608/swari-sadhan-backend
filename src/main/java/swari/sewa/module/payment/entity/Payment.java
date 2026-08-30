package swari.sewa.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import swari.sewa.module.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_transaction_uuid",
                columnNames = "transaction_uuid"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 100)
    private String transactionUuid;

    @Column(name = "gateway", nullable = false, length = 50)
    private String gateway;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_ref_id", length = 100)
    private String gatewayRefId;

    @Column(name = "shop_owner_id", nullable = false)
    private Long shopOwnerId;

    @Column(name = "subscription_plan_id", nullable = false)
    private Long subscriptionPlanId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "billing_cycle", length = 30)
    private String billingCycle;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    // Coupon snapshot — preserves historical coupon info even if admin edits/deletes the coupon
    @Column(name = "coupon_code_snapshot", length = 50)
    private String couponCodeSnapshot;

    @Column(name = "coupon_discount_type_snapshot", length = 20)
    private String couponDiscountTypeSnapshot;

    @Column(name = "coupon_discount_value_snapshot", length = 50)
    private String couponDiscountValueSnapshot;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "product_code", length = 50)
    private String productCode;

    // ===== Plan Snapshot (frozen at payment activation time) =====
    // These capture the plan name and subscription period at the moment the
    // payment was completed. The Subscription entity is reused on
    // renewal/upgrade (its snapshots are overwritten), so these Payment-level
    // snapshots are the only historically immutable record of what plan the
    // user had at each billing period.

    @Column(name = "plan_name_snapshot", length = 100)
    private String planNameSnapshot;

    @Column(name = "subscription_start_date_snapshot")
    private LocalDateTime subscriptionStartDateSnapshot;

    @Column(name = "subscription_end_date_snapshot")
    private LocalDateTime subscriptionEndDateSnapshot;

    // Total vehicle limit at the time of this payment (includes carry-forward).
    // Used by the Previous Plan UI to show the limit that was in effect.
    @Column(name = "vehicle_limit_snapshot")
    private Integer vehicleLimitSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currency == null) this.currency = "NPR";
        if (this.gateway == null) this.gateway = "ESEWA";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
