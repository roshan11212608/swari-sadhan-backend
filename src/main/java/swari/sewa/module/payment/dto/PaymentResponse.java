package swari.sewa.module.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String transactionUuid;
    private String gateway;
    private String gatewayTransactionId;
    private String gatewayRefId;
    private Long shopOwnerId;
    private Long subscriptionPlanId;
    private Long subscriptionId;
    private String billingCycle;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String invoiceNumber;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
}
