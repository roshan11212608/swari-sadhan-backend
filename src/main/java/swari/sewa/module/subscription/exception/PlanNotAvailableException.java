package swari.sewa.module.subscription.exception;

public class PlanNotAvailableException extends RuntimeException {
    public PlanNotAvailableException(String message) {
        super(message);
    }
}
