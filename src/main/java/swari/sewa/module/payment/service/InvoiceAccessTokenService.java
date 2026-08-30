package swari.sewa.module.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mints and validates short-lived, single-use tokens that authorise viewing one
 * specific invoice.
 *
 * <p><b>Why this exists.</b> The invoice is a browser-rendered HTML document
 * opened in a new tab, so the request cannot carry an {@code Authorization}
 * header. The previous implementation therefore accepted the caller's full JWT
 * as a {@code ?token=} query parameter. That leaks a long-lived, fully-scoped
 * credential into browser history, referrer headers, proxy logs and access logs.
 *
 * <p><b>What replaces it.</b> The client first calls the token endpoint using its
 * normal {@code Authorization} header. That returns an opaque token which:
 * <ul>
 *   <li>is bound to a single {@code transactionUuid} — it cannot be replayed
 *       against another invoice;</li>
 *   <li>is bound to the requesting subject, so ownership is still enforced when
 *       the token is redeemed;</li>
 *   <li>expires in {@value #TTL_SECONDS} seconds;</li>
 *   <li>is single-use — redeeming it consumes it;</li>
 *   <li>grants nothing except reading that one invoice.</li>
 * </ul>
 *
 * <p>Token layout: {@code base64url(payload) + "." + base64url(hmacSha256(payload))}
 * where payload is {@code transactionUuid|subjectType|subjectId|expiryEpochSec|nonce}.
 * The signature means we do not need to persist the token itself; only the
 * consumed nonces are tracked.
 *
 * <p><b>Deployment note.</b> Single-use enforcement uses an in-process nonce set.
 * On a single instance this is exact. If this service is ever scaled horizontally,
 * move {@link #consumedNonces} to shared storage (Redis) or accept that a token
 * becomes reusable-once-per-instance within its short TTL.
 */
@Service
@Slf4j
public class InvoiceAccessTokenService {

    /** Tokens are meant to be redeemed immediately by a browser navigation. */
    private static final int TTL_SECONDS = 120;

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String DELIMITER = "|";

    private final byte[] signingKey;
    private final SecureRandom random = new SecureRandom();

    /** nonce -> expiry epoch seconds, for single-use enforcement. */
    private final Map<String, Long> consumedNonces = new ConcurrentHashMap<>();

    public InvoiceAccessTokenService(@Value("${jwt.secret}") String secret) {
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Who a token was issued to. Redemption re-checks authorisation for this subject. */
    public enum SubjectType { SUPERADMIN, SHOP_OWNER }

    public record InvoiceAccess(String transactionUuid, SubjectType subjectType, Long subjectId) {}

    /**
     * Issue a token scoped to one invoice for one subject.
     *
     * @param transactionUuid the invoice's transaction UUID
     * @param subjectType     SUPERADMIN or SHOP_OWNER
     * @param subjectId       shop owner id, or the admin user id for super admins
     */
    public String issue(String transactionUuid, SubjectType subjectType, Long subjectId) {
        long expiry = Instant.now().getEpochSecond() + TTL_SECONDS;
        String nonce = newNonce();
        String payload = String.join(DELIMITER,
                transactionUuid,
                subjectType.name(),
                subjectId == null ? "" : subjectId.toString(),
                Long.toString(expiry),
                nonce);
        return encode(payload) + "." + encode(sign(payload));
    }

    /**
     * Validate and consume a token.
     *
     * @return the access it grants, or {@code null} if the token is malformed,
     *         tampered with, expired, already used, or scoped to a different invoice.
     */
    public InvoiceAccess redeem(String token, String expectedTransactionUuid) {
        if (token == null || token.isBlank() || expectedTransactionUuid == null) {
            return null;
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return null;
        }

        String payload;
        byte[] providedSignature;
        try {
            payload = new String(Base64.getUrlDecoder().decode(token.substring(0, dot)), StandardCharsets.UTF_8);
            providedSignature = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Constant-time comparison so signature validation does not leak timing.
        if (!MessageDigest.isEqual(sign(payload), providedSignature)) {
            log.warn("Invoice access token rejected: bad signature");
            return null;
        }

        String[] parts = payload.split("\\" + DELIMITER, -1);
        if (parts.length != 5) {
            return null;
        }
        String transactionUuid = parts[0];
        String subjectTypeRaw = parts[1];
        String subjectIdRaw = parts[2];
        String expiryRaw = parts[3];
        String nonce = parts[4];

        // Scope check: a token for invoice A must not open invoice B.
        if (!expectedTransactionUuid.equals(transactionUuid)) {
            log.warn("Invoice access token rejected: scoped to a different invoice");
            return null;
        }

        long expiry;
        SubjectType subjectType;
        try {
            expiry = Long.parseLong(expiryRaw);
            subjectType = SubjectType.valueOf(subjectTypeRaw);
        } catch (IllegalArgumentException e) {
            return null;
        }

        long nowSec = Instant.now().getEpochSecond();
        if (nowSec > expiry) {
            log.warn("Invoice access token rejected: expired");
            return null;
        }

        purgeExpiredNonces(nowSec);
        // putIfAbsent returns non-null when the nonce was already recorded, which
        // means this token has been redeemed before.
        if (consumedNonces.putIfAbsent(nonce, expiry) != null) {
            log.warn("Invoice access token rejected: already used");
            return null;
        }

        Long subjectId = subjectIdRaw.isEmpty() ? null : Long.valueOf(subjectIdRaw);
        return new InvoiceAccess(transactionUuid, subjectType, subjectId);
    }

    public int ttlSeconds() {
        return TTL_SECONDS;
    }

    private String newNonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGO));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign invoice access token", e);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void purgeExpiredNonces(long nowSec) {
        if (consumedNonces.size() > 1000) {
            consumedNonces.entrySet().removeIf(e -> e.getValue() < nowSec);
        }
    }
}
