package swari.sewa.common.exception;

/**
 * Thrown when an OTP verification fails (invalid, expired, or too many
 * attempts). The message is safe to return to the client.
 */
public class OtpVerificationException extends RuntimeException {
    public OtpVerificationException(String message) {
        super(message);
    }
}
