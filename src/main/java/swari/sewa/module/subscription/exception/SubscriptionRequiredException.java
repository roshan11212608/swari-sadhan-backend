package swari.sewa.module.subscription.exception;

/**
 * Thrown when a shop owner attempts a subscription-gated operation
 * (add vehicle, sell vehicle, etc.) without an active subscription
 * or active trial.
 */
public class SubscriptionRequiredException extends RuntimeException {

    private final String code;

    public SubscriptionRequiredException(String message) {
        super(message);
        this.code = "SUBSCRIPTION_REQUIRED";
    }

    public String getCode() {
        return code;
    }
}
