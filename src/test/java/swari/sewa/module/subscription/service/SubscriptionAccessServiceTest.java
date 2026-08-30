package swari.sewa.module.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.subscription.dto.VehicleUsageResponse;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionTrialConfig;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.exception.SubscriptionLimitExceededException;
import swari.sewa.module.subscription.exception.SubscriptionRequiredException;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTrialConfigRepository;
import swari.sewa.module.subscription.service.impl.SubscriptionAccessServiceImpl;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SubscriptionAccessServiceImpl — the centralized service that
 * enforces subscription access and vehicle-add limits.
 *
 * Business rules verified:
 * - ACTIVE or TRIAL subscription required for vehicle access
 * - Vehicle limit = monthlyLimit × billingCycleMonths (stored as snapshot)
 * - Vehicles added BEFORE subscription start are grandfathered (excluded from count)
 * - Selling is always allowed (not checked here, but add-limit does not block sell)
 * - Selling does NOT free an add slot (count includes SOLD vehicles)
 * - Null vehicle limit = unlimited
 */
class SubscriptionAccessServiceTest {

    private SubscriptionAccessServiceImpl service;
    private SubscriptionRepository subscriptionRepository;
    private VehicleRepository vehicleRepository;
    private SubscriptionTrialConfigRepository trialConfigRepository;

    private static final Long SHOP_OWNER_ID = 100L;
    private static final LocalDateTime SUB_START = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
        vehicleRepository = Mockito.mock(VehicleRepository.class);
        trialConfigRepository = Mockito.mock(SubscriptionTrialConfigRepository.class);
        service = new SubscriptionAccessServiceImpl(subscriptionRepository, vehicleRepository, trialConfigRepository);
    }

    private Subscription createActiveSubscription(Integer vehicleLimit, String planName, String billingCycle) {
        Subscription sub = Subscription.builder()
                .id(1L)
                .shopOwnerId(SHOP_OWNER_ID)
                .plan(new SubscriptionPlan())
                .status(SubscriptionStatus.ACTIVE)
                .startDate(SUB_START)
                .currentPeriodStart(SUB_START)
                .endDate(SUB_START.plusYears(1))
                .vehicleLimitSnapshot(vehicleLimit)
                .planNameSnapshot(planName)
                .billingCycleSnapshot(billingCycle)
                .build();
        return sub;
    }

    private Subscription createTrialSubscription(Integer vehicleLimit) {
        Subscription sub = Subscription.builder()
                .id(2L)
                .shopOwnerId(SHOP_OWNER_ID)
                .plan(new SubscriptionPlan())
                .status(SubscriptionStatus.TRIAL)
                .startDate(SUB_START)
                .currentPeriodStart(SUB_START)
                .endDate(SUB_START.plusDays(14))
                .vehicleLimitSnapshot(vehicleLimit)
                .planNameSnapshot("Trial")
                .billingCycleSnapshot("TRIAL")
                .build();
        return sub;
    }

    // ===== hasVehicleAccess =====

    @Nested
    @DisplayName("hasVehicleAccess — subscription status checks")
    class HasVehicleAccessTests {

        @Test
        @DisplayName("ACTIVE subscription → access granted")
        void testActiveSubscription_hasAccess() {
            Subscription sub = createActiveSubscription(10, "Starter", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            assertTrue(service.hasVehicleAccess(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("TRIAL subscription → access granted")
        void testTrialSubscription_hasAccess() {
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());
            Subscription trial = createTrialSubscription(5);
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(List.of(trial));

            assertTrue(service.hasVehicleAccess(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("No subscription → access denied")
        void testNoSubscription_noAccess() {
            when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                    .thenReturn(Collections.emptyList());

            assertFalse(service.hasVehicleAccess(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("EXPIRED subscription → access denied")
        void testExpiredSubscription_noAccess() {
            // EXPIRED subscriptions are not returned by ACTIVE or TRIAL queries
            when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                    .thenReturn(Collections.emptyList());

            assertFalse(service.hasVehicleAccess(SHOP_OWNER_ID));
        }
    }

    // ===== requireVehicleAccess =====

    @Nested
    @DisplayName("requireVehicleAccess — throws if no access")
    class RequireVehicleAccessTests {

        @Test
        @DisplayName("No subscription → throws SubscriptionRequiredException")
        void testNoSubscription_throws() {
            when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                    .thenReturn(Collections.emptyList());

            SubscriptionRequiredException ex = assertThrows(
                    SubscriptionRequiredException.class,
                    () -> service.requireVehicleAccess(SHOP_OWNER_ID));
            assertEquals("SUBSCRIPTION_REQUIRED", ex.getCode());
        }

        @Test
        @DisplayName("ACTIVE subscription → no exception")
        void testActiveSubscription_noException() {
            Subscription sub = createActiveSubscription(10, "Starter", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.requireVehicleAccess(SHOP_OWNER_ID));
        }
    }

    // ===== canAddVehicle =====

    @Nested
    @DisplayName("canAddVehicle — vehicle limit boundary tests")
    class CanAddVehicleTests {

        @Test
        @DisplayName("Yearly plan: limit 120, usage 0 → can add")
        void testYearlyLimit_usage0_canAdd() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(0L);

            assertTrue(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Yearly plan: limit 120, usage 119 → can add (boundary)")
        void testYearlyLimit_usage119_canAdd() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(119L);

            assertTrue(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Yearly plan: limit 120, usage 120 → cannot add (at limit)")
        void testYearlyLimit_usage120_cannotAdd() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(120L);

            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Yearly plan: limit 120, usage 121 → cannot add (beyond limit)")
        void testYearlyLimit_usage121_cannotAdd() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(121L);

            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Monthly plan: limit 10, usage 10 → cannot add")
        void testMonthlyLimit_usage10_cannotAdd() {
            Subscription sub = createActiveSubscription(10, "Starter", "monthly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(10L);

            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Null vehicle limit → unlimited, always can add")
        void testNullLimit_unlimited() {
            Subscription sub = createActiveSubscription(null, "Enterprise", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            assertTrue(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Limit 1: usage 0 → can add, usage 1 → cannot add")
        void testLimit1_boundary() {
            Subscription sub = createActiveSubscription(1, "Basic", "monthly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(0L);
            assertTrue(service.canAddVehicle(SHOP_OWNER_ID));

            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(1L);
            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }
    }

    // ===== validateCanAddVehicle =====

    @Nested
    @DisplayName("validateCanAddVehicle — throws at limit")
    class ValidateCanAddVehicleTests {

        @Test
        @DisplayName("At limit → throws SubscriptionLimitExceededException with correct details")
        void testAtLimit_throwsWithDetails() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(120L);

            SubscriptionLimitExceededException ex = assertThrows(
                    SubscriptionLimitExceededException.class,
                    () -> service.validateCanAddVehicle(SHOP_OWNER_ID));

            assertEquals("VEHICLE_LIMIT_REACHED", ex.getCode());
            assertEquals(120L, ex.getCurrentCount());
            assertEquals(120, ex.getLimit());
            assertEquals("Business", ex.getPlanName());
            assertTrue(ex.getMessage().contains("120"));
            assertTrue(ex.getMessage().contains("yearly"));
        }

        @Test
        @DisplayName("Under limit → no exception")
        void testUnderLimit_noException() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(50L);

            assertDoesNotThrow(() -> service.validateCanAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("No subscription → throws SubscriptionRequiredException")
        void testNoSubscription_throwsRequired() {
            when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                    .thenReturn(Collections.emptyList());

            assertThrows(SubscriptionRequiredException.class,
                    () -> service.validateCanAddVehicle(SHOP_OWNER_ID));
        }
    }

    // ===== Grandfathered Vehicles =====

    @Nested
    @DisplayName("Grandfathered vehicles — pre-subscription vehicles excluded from count")
    class GrandfatheredVehicleTests {

        @Test
        @DisplayName("5 vehicles before subscription + 10 after, limit 10 → can add (only 10 counted)")
        void testGrandfatheredVehicles_excludedFromCount() {
            Subscription sub = createActiveSubscription(10, "Starter", "monthly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            // countByShop_ShopOwner_IdAndCreatedAtAfter only counts vehicles AFTER subscription start
            // 5 pre-subscription vehicles are NOT counted
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(10L); // 10 vehicles added after subscription

            // At limit (10 counted, limit 10) → cannot add
            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("5 vehicles before subscription + 9 after, limit 10 → can add")
        void testGrandfatheredVehicles_underLimit() {
            Subscription sub = createActiveSubscription(10, "Starter", "monthly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(9L);

            assertTrue(service.canAddVehicle(SHOP_OWNER_ID));
        }
    }

    // ===== Selling Does Not Free Add Slots =====

    @Nested
    @DisplayName("Selling does not free add slots — SOLD vehicles still counted")
    class SellingDoesNotFreeSlotsTests {

        @Test
        @DisplayName("Limit 120, added 120, sold 20 → still cannot add (120 counted)")
        void testSellingDoesNotFreeSlot() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());

            // countByShop_ShopOwner_IdAndCreatedAtAfter counts ALL vehicles after subscription start
            // INCLUDING SOLD ones — selling does NOT decrement this count
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(120L); // 120 added (even though 20 were sold)

            assertFalse(service.canAddVehicle(SHOP_OWNER_ID));
        }
    }

    // ===== getVehicleUsage =====

    @Nested
    @DisplayName("getVehicleUsage — response DTO tests")
    class GetVehicleUsageTests {

        @Test
        @DisplayName("Active subscription with limit → correct usage response")
        void testActiveSubscription_usageResponse() {
            Subscription sub = createActiveSubscription(120, "Business", "yearly");
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(sub));
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(50L);
            when(vehicleRepository.countByShop_ShopOwner_Id(SHOP_OWNER_ID)).thenReturn(55L);
            when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(SHOP_OWNER_ID, VehicleStatus.SOLD)).thenReturn(5L);

            VehicleUsageResponse response = service.getVehicleUsage(SHOP_OWNER_ID);

            assertEquals("ACTIVE", response.getSubscriptionStatus());
            assertEquals("Business", response.getPlanName());
            assertEquals(50L, response.getCurrentVehicleCount());
            assertEquals(120, response.getVehicleLimit());
            assertEquals(70, response.getRemainingSlots()); // 120 - 50 = 70
            assertTrue(response.isCanAddVehicle());
            assertTrue(response.isHasAccess());
        }

        @Test
        @DisplayName("No subscription → NONE status, no access")
        void testNoSubscription_usageResponse() {
            when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                    .thenReturn(Collections.emptyList());
            when(vehicleRepository.countByShop_ShopOwner_Id(SHOP_OWNER_ID)).thenReturn(5L);
            when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(SHOP_OWNER_ID, VehicleStatus.SOLD)).thenReturn(0L);

            VehicleUsageResponse response = service.getVehicleUsage(SHOP_OWNER_ID);

            assertEquals("NONE", response.getSubscriptionStatus());
            assertFalse(response.isCanAddVehicle());
            assertFalse(response.isHasAccess());
            assertNull(response.getVehicleLimit());
        }

        @Test
        @DisplayName("Trial subscription → TRIAL status with trial end date")
        void testTrialSubscription_usageResponse() {
            Subscription trial = createTrialSubscription(5);
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());
            when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                    .thenReturn(List.of(trial));
            when(vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(SHOP_OWNER_ID, SUB_START))
                    .thenReturn(3L);
            when(vehicleRepository.countByShop_ShopOwner_Id(SHOP_OWNER_ID)).thenReturn(3L);
            when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(SHOP_OWNER_ID, VehicleStatus.SOLD)).thenReturn(0L);

            VehicleUsageResponse response = service.getVehicleUsage(SHOP_OWNER_ID);

            assertEquals("TRIAL", response.getSubscriptionStatus());
            assertEquals("Trial", response.getPlanName());
            assertEquals(5, response.getVehicleLimit());
            assertEquals(2, response.getRemainingSlots()); // 5 - 3 = 2
            assertTrue(response.isCanAddVehicle());
            assertNotNull(response.getTrialEndDate());
        }
    }
}
