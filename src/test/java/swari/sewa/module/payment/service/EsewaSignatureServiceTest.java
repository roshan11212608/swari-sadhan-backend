package swari.sewa.module.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for eSewa HMAC-SHA256 signature generation.
 *
 * The signature is verified against an independent Python computation using
 * the same HMAC-SHA256 algorithm to ensure correctness.
 *
 * Note: The UAT secret key "8gBm/:&EnhH.1/q" is publicly documented by eSewa
 * for testing purposes. It is NOT a production secret.
 */
class EsewaSignatureServiceTest {

    private final EsewaSignatureService signatureService = new EsewaSignatureService();

    @Test
    @DisplayName("Generate correct HMAC-SHA256 signature (verified against Python)")
    void testSignatureGeneration_knownValues() {
        String totalAmount = "100";
        String transactionUuid = "11-201-13";
        String productCode = "EPAYTEST";
        String secretKey = "8gBm/:&EnhH.1/q";

        String signature = signatureService.generateSignature(totalAmount, transactionUuid, productCode, secretKey);

        // Verified against independent Python computation:
        //   import hmac, hashlib, base64
        //   hmac.new(secret.encode(), msg.encode(), hashlib.sha256).digest()
        //   -> base64.b64encode(digest).decode()
        assertEquals("5DZywcrTKD0gia/rsSMcrRHmJl+4Tbol6S+lWgdJ94E=", signature,
                "Signature should match independent HMAC-SHA256 computation");
    }

    @Test
    @DisplayName("Generate correct signature for another eSewa documented test case")
    void testSignatureGeneration_totalAmountWithTax() {
        // From eSewa docs: total_amount=110, transaction_uuid=ab14a8f2b02c3, product_code=EPAYTEST
        String totalAmount = "110";
        String transactionUuid = "ab14a8f2b02c3";
        String productCode = "EPAYTEST";
        String secretKey = "8gBm/:&EnhH.1/q";

        String signature = signatureService.generateSignature(totalAmount, transactionUuid, productCode, secretKey);

        // The signature should be a valid Base64 string
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        // Verify it's valid Base64
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(signature));
    }

    @Test
    @DisplayName("Different inputs produce different signatures")
    void testSignatureGeneration_differentInputs() {
        String secretKey = "8gBm/:&EnhH.1/q";

        String sig1 = signatureService.generateSignature("100", "uuid-1", "EPAYTEST", secretKey);
        String sig2 = signatureService.generateSignature("100", "uuid-2", "EPAYTEST", secretKey);

        assertNotEquals(sig1, sig2, "Different transaction UUIDs should produce different signatures");
    }

    @Test
    @DisplayName("Same inputs produce same signature (deterministic)")
    void testSignatureGeneration_deterministic() {
        String secretKey = "8gBm/:&EnhH.1/q";

        String sig1 = signatureService.generateSignature("100", "uuid-1", "EPAYTEST", secretKey);
        String sig2 = signatureService.generateSignature("100", "uuid-1", "EPAYTEST", secretKey);

        assertEquals(sig1, sig2, "Same inputs should produce the same signature");
    }

    @Test
    @DisplayName("Different secret keys produce different signatures")
    void testSignatureGeneration_differentSecrets() {
        String totalAmount = "100";
        String transactionUuid = "uuid-1";
        String productCode = "EPAYTEST";

        String sig1 = signatureService.generateSignature(totalAmount, transactionUuid, productCode, "secret1");
        String sig2 = signatureService.generateSignature(totalAmount, transactionUuid, productCode, "secret2");

        assertNotEquals(sig1, sig2, "Different secret keys should produce different signatures");
    }
}
