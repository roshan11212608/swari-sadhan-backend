package swari.sewa.module.subscription.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionSettingsResponse {
    private Long id;
    private Integer defaultTrialDays;
    private Integer taxPercentage;
    private String currency;
    private String invoicePrefix;
    private Integer paymentReminderDays;
    private Integer renewalReminder;
    private Integer gracePeriod;
    private String cancellationPolicy;
    private String refundPolicy;
    private Boolean enableAutoRenewal;
    private Boolean enableFreeTrial;
    private Boolean enableCoupons;
    private Boolean enableLifetimePlans;
    private Boolean enableVat;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
