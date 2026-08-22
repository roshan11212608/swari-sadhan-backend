package swari.sewa.module.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EsewaSignatureService {

    /**
     * Generates the HMAC-SHA256 signature for eSewa ePay V2.
     *
     * Signed fields (in order): total_amount, transaction_uuid, product_code
     * Message format: total_amount=<val>,transaction_uuid=<val>,product_code=<val>
     *
     * @param totalAmount     the total amount as a plain string (e.g. "110")
     * @param transactionUuid the unique transaction UUID
     * @param productCode     the eSewa product/merchant code
     * @param secretKey       the eSewa secret key (never logged)
     * @return Base64-encoded HMAC-SHA256 signature
     */
    public String generateSignature(String totalAmount, String transactionUuid, String productCode, String secretKey) {
        String message = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s",
                totalAmount, transactionUuid, productCode);
        log.debug("Generating eSewa signature for transaction_uuid: {}", transactionUuid);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            log.error("Failed to generate eSewa signature for transaction_uuid: {}", transactionUuid, e);
            throw new RuntimeException("Failed to generate payment signature", e);
        }
    }
}
