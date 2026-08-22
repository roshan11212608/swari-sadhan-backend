package swari.sewa.module.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreatePaymentResponse {

    private String paymentUrl;
    private String transactionUuid;
    private String productCode;
    private String amount;
    private String taxAmount;
    private String totalAmount;
    private String signedFieldNames;
    private String signature;
    private String successUrl;
    private String failureUrl;
    private String productServiceCharge;
    private String productDeliveryCharge;
    private String currency;

    // For frontend reference
    private Long paymentId;
    private BigDecimal amountValue;
    private BigDecimal totalAmountValue;
}
