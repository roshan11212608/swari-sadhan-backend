package swari.sewa.common.exception;

/**
 * Thrown when a signup is attempted with a mobile number that is already
 * registered to an active account.
 */
public class MobileNumberAlreadyExistsException extends RuntimeException {
    public MobileNumberAlreadyExistsException(String message) {
        super(message);
    }
}
