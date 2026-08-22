package swari.sewa.module.payment.service;

import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.CreatePaymentResponse;
import swari.sewa.module.payment.dto.PaymentResponse;

public interface EsewaPaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request, Long shopOwnerId);

    PaymentResponse handleSuccessCallback(String transactionUuid, String totalAmount, String transactionCode, String refId, String status);

    PaymentResponse handleFailureCallback(String transactionUuid, String failureReason);

    PaymentResponse getPaymentByTransactionUuid(String transactionUuid);

    /**
     * Regenerate eSewa form parameters for an existing PENDING payment.
     * Used by the QR code flow — the QR encodes a URL that renders an auto-submitting form.
     */
    CreatePaymentResponse getEsewaFormParams(String transactionUuid);
}
