package swari.sewa.module.subscription.exception;

/**
 * Thrown when a shop owner has reached their plan's vehicle limit.
 */
public class SubscriptionLimitExceededException extends RuntimeException {

    private final String code;
    private final long currentCount;
    private final Integer limit;
    private final String planName;

    public SubscriptionLimitExceededException(String message) {
        super(message);
        this.code = "VEHICLE_LIMIT_REACHED";
        this.currentCount = 0;
        this.limit = null;
        this.planName = null;
    }

    public SubscriptionLimitExceededException(String message, long currentCount, Integer limit, String planName) {
        super(message);
        this.code = "VEHICLE_LIMIT_REACHED";
        this.currentCount = currentCount;
        this.limit = limit;
        this.planName = planName;
    }

    public String getCode() {
        return code;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getPlanName() {
        return planName;
    }
}
