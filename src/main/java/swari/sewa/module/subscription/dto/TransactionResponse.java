package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private Long shopOwnerId;
    private String shopName;
    private Long subscriptionId;
    private Long planId;
    private String planName;
    private BigDecimal amount;
    private BigDecimal tax;
    private String couponCode;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String gateway;
    private String status;
    private String invoiceNumber;
    private LocalDateTime transactionDate;
}
