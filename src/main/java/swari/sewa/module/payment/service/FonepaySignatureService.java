package swari.sewa.module.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class FonepaySignatureService {

    private static final String HMAC_SHA512 = "HmacSHA512";

    /**
     * Generates HMAC-SHA512 signature for Fonepay.
     * Output is hex uppercase (Fonepay's format).
     *
     * @param data      the formatted string to sign
     * @param secretKey the merchant secret key
     * @return hex uppercase signature
     */
    public String generateSignature(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            log.error("Failed to generate Fonepay signature", e);
            throw new RuntimeException("Failed to generate Fonepay signature", e);
        }
    }

    /**
     * Verifies a Fonepay callback signature.
     */
    public boolean verifySignature(String data, String secretKey, String expectedSignature) {
        String computed = generateSignature(data, secretKey);
        return computed.equalsIgnoreCase(expectedSignature);
    }

    private String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
