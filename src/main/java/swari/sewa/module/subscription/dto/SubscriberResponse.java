package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriberResponse {
    private Long id;
    private Long shopOwnerId;
    private String shopName;
    private String ownerName;
    private Long planId;
    private String currentPlan;
    private Boolean trial;
    private Long trialId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenewal;
    private String status;
    private LocalDateTime renewalDate;
    private LocalDateTime lastPaymentDate;
    private BigDecimal lastPaymentAmount;
    private UsageDto usage;
}
