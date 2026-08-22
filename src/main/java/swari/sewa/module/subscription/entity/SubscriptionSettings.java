package swari.sewa.module.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionSettings {

    @Id
    private Long id = 1L;

    @Column(name = "default_trial_days", nullable = false)
    @Builder.Default
    private Integer defaultTrialDays = 14;

    @Column(name = "tax_percentage", nullable = false)
    @Builder.Default
    private Integer taxPercentage = 18;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    @Column(name = "invoice_prefix", nullable = false)
    @Builder.Default
    private String invoicePrefix = "INV";

    @Column(name = "payment_reminder_days", nullable = false)
    @Builder.Default
    private Integer paymentReminderDays = 7;

    @Column(name = "renewal_reminder", nullable = false)
    @Builder.Default
    private Integer renewalReminder = 3;

    @Column(name = "grace_period", nullable = false)
    @Builder.Default
    private Integer gracePeriod = 5;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "refund_policy", columnDefinition = "TEXT")
    private String refundPolicy;

    @Column(name = "enable_auto_renewal")
    @Builder.Default
    private Boolean enableAutoRenewal = true;

    @Column(name = "enable_free_trial")
    @Builder.Default
    private Boolean enableFreeTrial = true;

    @Column(name = "enable_coupons")
    @Builder.Default
    private Boolean enableCoupons = true;

    @Column(name = "enable_lifetime_plans")
    @Builder.Default
    private Boolean enableLifetimePlans = true;

    @Column(name = "enable_vat")
    @Builder.Default
    private Boolean enableVat = true;

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
        SubscriptionSettings that = (SubscriptionSettings) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
