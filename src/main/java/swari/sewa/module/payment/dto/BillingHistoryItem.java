package swari.sewa.module.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BillingHistoryItem {
    private Long id;
    private String invoiceNumber;
    private String transactionUuid;
    private String gateway;
    private String status;       // SUCCESS, PENDING, FAILED
    private String billingCycle;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private String couponCode;       // snapshot from payment
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;
    private String planName;
    private Long subscriptionId;
}
