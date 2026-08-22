package swari.sewa.module.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CurrentSubscriptionResponse {

    // Subscription info
    private Long subscriptionId;
    private String status; // ACTIVE, EXPIRED, etc.

    // Plan info
    private Long planId;
    private String planName;
    private String planDescription;
    private String icon;
    private String themeColor;

    // Pricing
    private BigDecimal price;
    private String billingCycle; // monthly, quarterly, etc.
    private String currency;

    // Dates
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime renewalDate;
    private Integer daysUntilExpiry;

    // Usage limits (from plan restrictions)
    private Integer vehicleLimit;
    private Integer storageLimit; // in MB
    private String enquiryLimit; // "Unlimited" or number
    private Integer featuredLimit;

    // Actual usage counts
    private Integer vehiclesUsed;
    private Integer enquiriesUsed;
    private Integer featuredUsed;

    // Latest payment info
    private String invoiceNumber;
    private String paymentGateway;
    private String transactionUuid;
}
