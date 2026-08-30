package swari.sewa.module.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for vehicle usage information shown on the vehicle page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUsageResponse {

    /** "ACTIVE", "TRIAL", or "NONE" */
    private String subscriptionStatus;

    /** Plan name, or null if no subscription */
    private String planName;

    /** Current vehicle count (inventory — excludes SOLD vehicles) */
    private long currentVehicleCount;

    /** Vehicle limit from the plan, or null if unlimited/no subscription */
    private Integer vehicleLimit;

    /** Remaining slots, or null if unlimited. 0 if limit reached. */
    private Integer remainingSlots;

    /** True if the shop owner can add another vehicle */
    private boolean canAddVehicle;

    /** True if the shop owner has vehicle access (ACTIVE or TRIAL) */
    private boolean hasAccess;

    /** Trial end date in ISO format, or null if not a trial */
    private String trialEndDate;

    /** Days remaining in trial, or null if not a trial */
    private Integer trialDaysRemaining;

    /** Subscription start date in ISO format, or null if no subscription */
    private String startDate;

    /** Subscription end/renewal date in ISO format, or null if no subscription */
    private String endDate;

    /** Billing cycle (e.g. "monthly", "yearly", "TRIAL"), or null if no subscription */
    private String billingCycle;

    /** Days remaining until end/renewal date, or null if no subscription */
    private Integer daysRemaining;

    /** Price paid for the current subscription period, or null if no subscription */
    private BigDecimal pricePaid;

    // ===== Vehicle allowance rollover fields =====

    /** Base vehicle limit from the plan (monthly limit × cycle months), excluding carry-forward */
    private Integer newPlanVehicleLimit;

    /** Unused vehicle allowance carried forward from the previous billing period */
    private Integer carriedForwardVehicleLimit;

    /** Total vehicle limit = newPlanVehicleLimit + carriedForwardVehicleLimit. Same as vehicleLimit. */
    private Integer totalVehicleLimit;

    /** Vehicles used in the current billing period (counted from currentPeriodStart) */
    private Long vehiclesUsed;

    /** Vehicles remaining = totalVehicleLimit - vehiclesUsed */
    private Integer vehiclesRemaining;
}
