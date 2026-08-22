package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateSubscriptionSettingsRequest {
    @PositiveOrZero private Integer defaultTrialDays;
    @PositiveOrZero private Integer taxPercentage;
    @NotBlank private String currency;
    @NotBlank private String invoicePrefix;
    @PositiveOrZero private Integer paymentReminderDays;
    @PositiveOrZero private Integer renewalReminder;
    @PositiveOrZero private Integer gracePeriod;
    private String cancellationPolicy;
    private String refundPolicy;
    private Boolean enableAutoRenewal;
    private Boolean enableFreeTrial;
    private Boolean enableCoupons;
    private Boolean enableLifetimePlans;
    private Boolean enableVat;
}
