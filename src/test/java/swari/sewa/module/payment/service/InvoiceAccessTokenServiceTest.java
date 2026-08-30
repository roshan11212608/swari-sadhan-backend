package swari.sewa.module.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import swari.sewa.module.payment.service.InvoiceAccessTokenService.InvoiceAccess;
import swari.sewa.module.payment.service.InvoiceAccessTokenService.SubjectType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InvoiceAccessTokenService}.
 *
 * Security rules under test:
 *   1. A token is bound to ONE transactionUuid — it cannot open another invoice.
 *   2. A token is SINGLE USE — the nonce is consumed on first redemption.
 *   3. A token is HMAC-signed — any mutation of payload or signature, or a token
 *      signed with a different secret, is rejected.
 *   4. Malformed/blank/null input is rejected without throwing.
 *   5. Redemption round-trips the subject (type + id) so ownership can be
 *      re-checked by the caller.
 *
 * Every failure mode returns {@code null} rather than throwing.
 */
class InvoiceAccessTokenServiceTest {

    private static final String SECRET = "some-test-secret-key-that-is-long-enough";
    private static final String TXN = "TXN-A";

    private InvoiceAccessTokenService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceAccessTokenService(SECRET);
    }

    @Test
    @DisplayName("Freshly issued token redeems and returns the correct uuid, subject type and subject id")
    void freshToken_redeemsSuccessfully() {
        String token = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);

        InvoiceAccess access = service.redeem(token, TXN);

        assertNotNull(access, "A freshly issued token must redeem");
        assertEquals(TXN, access.transactionUuid());
        assertEquals(SubjectType.SHOP_OWNER, access.subjectType());
        assertEquals(7L, access.subjectId());
    }

    @Test
    @DisplayName("SINGLE USE: the same token cannot be redeemed twice")
    void token_isSingleUse() {
        String token = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);

        assertNotNull(service.redeem(token, TXN), "First redemption must succeed");
        assertNull(service.redeem(token, TXN), "Second redemption must be refused (nonce consumed)");
    }

    @Test
    @DisplayName("SCOPE: a token issued for TXN-A is refused when presented for TXN-B")
    void token_isScopedToOneInvoice() {
        String token = service.issue("TXN-A", SubjectType.SHOP_OWNER, 7L);

        assertNull(service.redeem(token, "TXN-B"),
                "A token for one invoice must not open a different invoice");
    }

    @Test
    @DisplayName("TAMPERING: mutating the payload segment invalidates the signature")
    void tamperedPayload_isRejected() {
        String token = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);
        int dot = token.lastIndexOf('.');
        String payload = token.substring(0, dot);
        String signature = token.substring(dot + 1);

        String tamperedPayload = mutate(payload);
        assertNotEquals(payload, tamperedPayload);

        assertNull(service.redeem(tamperedPayload + "." + signature, TXN),
                "A mutated payload must be rejected");
    }

    @Test
    @DisplayName("TAMPERING: mutating the signature segment is rejected")
    void tamperedSignature_isRejected() {
        String token = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);
        int dot = token.lastIndexOf('.');
        String payload = token.substring(0, dot);
        String signature = token.substring(dot + 1);

        String tamperedSignature = mutate(signature);
        assertNotEquals(signature, tamperedSignature);

        assertNull(service.redeem(payload + "." + tamperedSignature, TXN),
                "A forged signature must be rejected");
    }

    @Test
    @DisplayName("null token is rejected")
    void nullToken_isRejected() {
        assertNull(service.redeem(null, TXN));
    }

    @Test
    @DisplayName("blank and empty tokens are rejected")
    void blankToken_isRejected() {
        assertNull(service.redeem("", TXN));
        assertNull(service.redeem("   ", TXN));
    }

    @Test
    @DisplayName("Malformed token with no '.' separator is rejected")
    void tokenWithoutSeparator_isRejected() {
        assertNull(service.redeem("thisisnotavalidtokenatall", TXN));
        // A leading dot leaves an empty payload, also invalid.
        assertNull(service.redeem(".signatureonly", TXN));
        // A trailing dot leaves an empty signature, also invalid.
        assertNull(service.redeem("payloadonly.", TXN));
    }

    @Test
    @DisplayName("Garbage base64 is rejected without throwing")
    void garbageBase64_isRejected() {
        assertNull(service.redeem("!!!not-base64!!!.@@@also-not-base64@@@", TXN));
        assertNull(service.redeem("****.****", TXN));
    }

    @Test
    @DisplayName("A token signed with a DIFFERENT secret is rejected")
    void tokenFromForeignSecret_isRejected() {
        InvoiceAccessTokenService issuer = new InvoiceAccessTokenService("secret-number-one-that-is-long-enough");
        InvoiceAccessTokenService verifier = new InvoiceAccessTokenService("secret-number-two-that-is-long-enough");

        String token = issuer.issue(TXN, SubjectType.SHOP_OWNER, 7L);

        assertNotNull(issuer.redeem(token, TXN), "Sanity: the issuer itself accepts its own token");
        assertNull(verifier.redeem(issuer.issue(TXN, SubjectType.SHOP_OWNER, 7L), TXN),
                "A token signed with another secret must be rejected");
    }

    @Test
    @DisplayName("SUPERADMIN with a null subjectId round-trips as null (not 0, not an exception)")
    void superadminWithNullSubjectId_roundTrips() {
        String token = service.issue(TXN, SubjectType.SUPERADMIN, null);

        InvoiceAccess access = service.redeem(token, TXN);

        assertNotNull(access);
        assertEquals(SubjectType.SUPERADMIN, access.subjectType());
        assertNull(access.subjectId(), "An absent subject id must decode back to null");
        assertEquals(TXN, access.transactionUuid());
    }

    @Test
    @DisplayName("SHOP_OWNER with a concrete subjectId round-trips exactly")
    void shopOwnerWithSubjectId_roundTrips() {
        String token = service.issue(TXN, SubjectType.SHOP_OWNER, 42L);

        InvoiceAccess access = service.redeem(token, TXN);

        assertNotNull(access);
        assertEquals(SubjectType.SHOP_OWNER, access.subjectType());
        assertEquals(42L, access.subjectId());
    }

    @Test
    @DisplayName("ttlSeconds() is positive so tokens are short-lived but usable")
    void ttlSeconds_isPositive() {
        assertTrue(service.ttlSeconds() > 0, "TTL must be positive, was " + service.ttlSeconds());
    }

    @Test
    @DisplayName("Two tokens for the same invoice are distinct (nonce) and each redeems independently")
    void twoTokensForSameInvoice_areIndependent() {
        String first = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);
        String second = service.issue(TXN, SubjectType.SHOP_OWNER, 7L);

        assertNotEquals(first, second, "A per-token nonce must make each token unique");

        assertNotNull(service.redeem(first, TXN), "First token must redeem");
        assertNotNull(service.redeem(second, TXN), "Second token must redeem independently");
    }

    /**
     * Flip every character of a base64url segment to a different valid base64url
     * character, guaranteeing a different segment while keeping it decodable-ish.
     */
    private static String mutate(String segment) {
        StringBuilder out = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            out.append(c == 'A' ? 'B' : 'A');
        }
        return out.toString();
    }
}
