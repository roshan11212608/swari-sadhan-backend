package swari.sewa.common.exception;

/**
 * Thrown when an OTP operation is rejected because the caller must wait
 * before retrying (e.g. resend cooldown, rate limit).
 */
public class OtpRateLimitException extends RuntimeException {
    public OtpRateLimitException(String message) {
        super(message);
    }
}
