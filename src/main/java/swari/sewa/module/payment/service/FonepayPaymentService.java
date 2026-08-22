package swari.sewa.module.payment.service;

import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.PaymentResponse;

public interface FonepayPaymentService {

    /**
     * Create a Fonepay payment and return the signed redirect URL.
     * The user is redirected to Fonepay's page where they can scan QR with eSewa/Khalti/etc.
     */
    String createPayment(CreatePaymentRequest request, Long shopOwnerId);

    /**
     * Handle Fonepay callback after payment.
     * Verifies the signature, checks status, activates subscription.
     */
    PaymentResponse handleCallback(String prn, String pid, String ps, String amt,
                                    String uid, String bid, String dv);

    /**
     * Get payment status by transaction UUID (PRN).
     */
    PaymentResponse getPaymentByPrn(String prn);
}
