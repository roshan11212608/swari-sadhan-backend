package swari.sewa.common.exception;

public class RegistrationNumberAlreadyExistsException extends RuntimeException {
    public RegistrationNumberAlreadyExistsException(String message) {
        super(message);
    }
}
