package swari.sewa.module.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import swari.sewa.module.subscription.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_owner_id", nullable = false)
    private Long shopOwnerId;

    @Column(name = "shop_id")
    private Long shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "trial_id")
    private Long trialId;

    // ===== Plan Snapshot (frozen at purchase time) =====
    // These fields capture the plan details at the moment of subscription
    // so that later admin changes (edit, delete, unpublish, modify) do not
    // affect existing subscriptions.

    @Column(name = "plan_name_snapshot")
    private String planNameSnapshot;

    @Column(name = "plan_description_snapshot", columnDefinition = "TEXT")
    private String planDescriptionSnapshot;

    @Column(name = "plan_icon_snapshot")
    private String planIconSnapshot;

    @Column(name = "plan_theme_color_snapshot")
    private String planThemeColorSnapshot;

    @Column(name = "vehicle_limit_snapshot")
    private Integer vehicleLimitSnapshot;

    @Column(name = "price_paid", precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "billing_cycle_snapshot")
    private String billingCycleSnapshot;

    // ===== Dates =====

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    @Column(name = "auto_renewal")
    private Boolean autoRenewal = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    @Column(name = "suspended_date")
    private LocalDateTime suspendedDate;

    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
