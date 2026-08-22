package swari.sewa.module.subscription.service;

import swari.sewa.module.subscription.dto.VehicleUsageResponse;

/**
 * Centralized subscription access checks for shop-owner operations.
 *
 * <p>A shop owner has vehicle-management access when they have an
 * ACTIVE subscription OR a TRIAL subscription. EXPIRED, CANCELLED,
 * and SUSPENDED subscriptions do NOT grant access.
 */
public interface SubscriptionAccessService {

    /**
     * Returns true if the shop owner has an ACTIVE subscription.
     */
    boolean hasActiveSubscription(Long shopOwnerId);

    /**
     * Returns true if the shop owner has an active TRIAL subscription.
     */
    boolean hasActiveTrial(Long shopOwnerId);

    /**
     * Returns true if the shop owner has an ACTIVE or TRIAL subscription.
     */
    boolean hasVehicleAccess(Long shopOwnerId);

    /**
     * Ensures the shop owner has an ACTIVE or TRIAL subscription.
     * Throws SubscriptionRequiredException if not.
     */
    void requireVehicleAccess(Long shopOwnerId);

    /**
     * Checks whether the shop owner can add another vehicle.
     * Combines subscription-access check + vehicle-limit check.
     */
    boolean canAddVehicle(Long shopOwnerId);

    /**
     * Validates that the shop owner can add another vehicle.
     * Throws SubscriptionRequiredException or SubscriptionLimitExceededException.
     */
    void validateCanAddVehicle(Long shopOwnerId);

    /**
     * Returns the current vehicle usage for the shop owner:
     * current count, limit, plan name, subscription status.
     */
    VehicleUsageResponse getVehicleUsage(Long shopOwnerId);
}
