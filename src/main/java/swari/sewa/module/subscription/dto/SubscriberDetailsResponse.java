package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriberDetailsResponse {
    private Long id;
    private Long shopOwnerId;
    private String shopName;
    private String ownerName;
    private Long planId;
    private String currentPlan;
    private String billingCycle;
    private Boolean trial;
    private Long trialId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenewal;
    private String status;
    private LocalDateTime renewalDate;
    private LocalDateTime lastPaymentDate;
    private BigDecimal lastPaymentAmount;
    private String lastTransactionUuid;
    private String lastInvoiceNumber;
    private UsageDto usage;
    private String email;
    private String phone;
    private LocalDateTime cancelledDate;
    private LocalDateTime suspendedDate;
    private String reason;
}
