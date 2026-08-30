package swari.sewa.module.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionTrialConfig;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.SubscriptionPlanRepository;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTrialConfigRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrialSubscriptionService — the service that auto-starts
 * a free trial subscription for newly-approved shop owners.
 *
 * Business rules verified:
 * - Trial starts only if no active subscription exists
 * - Trial config is singleton (id=1)
 * - Trial duration comes from config
 * - Trial vehicle limit overrides plan limit when configured
 * - Trial plan falls back to first published plan if not configured
 * - Trial status = TRIAL
 * - Duplicate trial creation is handled (DataIntegrityViolationException swallowed)
 * - Trial creation failure does NOT block the calling flow (REQUIRES_NEW)
 */
class TrialSubscriptionServiceTest {

    private TrialSubscriptionService service;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionTrialConfigRepository trialConfigRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private ShopOwnerRepository shopOwnerRepository;
    private EmailService emailService;

    private static final Long SHOP_OWNER_ID = 100L;

    @BeforeEach
    void setUp() {
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
        trialConfigRepository = Mockito.mock(SubscriptionTrialConfigRepository.class);
        subscriptionPlanRepository = Mockito.mock(SubscriptionPlanRepository.class);
        shopOwnerRepository = Mockito.mock(ShopOwnerRepository.class);
        emailService = Mockito.mock(EmailService.class);

        service = new TrialSubscriptionService(
                subscriptionRepository, trialConfigRepository,
                subscriptionPlanRepository, shopOwnerRepository, emailService);
    }

    private SubscriptionTrialConfig createTrialConfig(Integer duration, Integer vehicleLimit, Long trialPlanId, boolean active) {
        return SubscriptionTrialConfig.builder()
                .id(1L)
                .name("Free Trial")
                .description("14-day free trial")
                .duration(duration)
                .vehicleLimit(vehicleLimit)
                .trialPlanId(trialPlanId)
                .active(active)
                .build();
    }

    private SubscriptionPlan createPublishedPlan(Long id, String name, Integer maxVehicles) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .status(PlanStatus.PUBLISHED)
                .build();

        if (maxVehicles != null) {
            SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                    .plan(plan)
                    .maxVehicles(maxVehicles)
                    .build();
            Set<SubscriptionPlanRestriction> restrictions = new HashSet<>();
            restrictions.add(restriction);
            plan.setRestrictions(restrictions);
        }

        return plan;
    }

    private ShopOwner createShopOwner(Long id) {
        ShopOwner owner = new ShopOwner();
        owner.setId(id);
        owner.setEmail("test@example.com");
        owner.setShopName("Test Shop");
        return owner;
    }

    // ===== Trial Creation =====

    @Test
    @DisplayName("Eligible shop owner → trial created with correct duration and status")
    void testTrialCreated_success() {
        SubscriptionTrialConfig config = createTrialConfig(14, 5, 7L, true);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                .thenReturn(Collections.emptyList());
        SubscriptionPlan plan = createPublishedPlan(7L, "Starter", 10);
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(shopOwnerRepository.findById(SHOP_OWNER_ID)).thenReturn(Optional.of(createShopOwner(SHOP_OWNER_ID)));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertTrue(result);

        verify(subscriptionRepository).save(argThat(s -> {
            // Verify end date is approximately 14 days from now
            long daysBetween = java.time.Duration.between(
                    LocalDateTime.now(), s.getEndDate()).toDays();
            return s.getStatus() == SubscriptionStatus.TRIAL &&
                    s.getShopOwnerId().equals(SHOP_OWNER_ID) &&
                    s.getPlanNameSnapshot() != null &&
                    daysBetween >= 13 && daysBetween <= 15; // allow slight time variance
        }));
    }

    @Test
    @DisplayName("Trial vehicle limit overrides plan limit when configured")
    void testTrialVehicleLimit_overridesPlan() {
        SubscriptionTrialConfig config = createTrialConfig(14, 5, 7L, true);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                .thenReturn(Collections.emptyList());
        // Plan has maxVehicles=10, but trial config has vehicleLimit=5
        SubscriptionPlan plan = createPublishedPlan(7L, "Starter", 10);
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(shopOwnerRepository.findById(SHOP_OWNER_ID)).thenReturn(Optional.of(createShopOwner(SHOP_OWNER_ID)));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        service.startTrialIfNeeded(SHOP_OWNER_ID);

        // Trial-specific vehicle limit (5) should be used, not plan limit (10)
        verify(subscriptionRepository).save(argThat(s ->
                s.getVehicleLimitSnapshot() != null &&
                s.getVehicleLimitSnapshot() == 5
        ));
    }

    @Test
    @DisplayName("Trial vehicle limit null → falls back to plan limit")
    void testTrialVehicleLimit_null_fallsBackToPlan() {
        SubscriptionTrialConfig config = createTrialConfig(14, null, 7L, true);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                .thenReturn(Collections.emptyList());
        SubscriptionPlan plan = createPublishedPlan(7L, "Starter", 10);
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(shopOwnerRepository.findById(SHOP_OWNER_ID)).thenReturn(Optional.of(createShopOwner(SHOP_OWNER_ID)));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        service.startTrialIfNeeded(SHOP_OWNER_ID);

        verify(subscriptionRepository).save(argThat(s ->
                s.getVehicleLimitSnapshot() != null &&
                s.getVehicleLimitSnapshot() == 10 // plan limit
        ));
    }

    // ===== Trial Not Created =====

    @Test
    @DisplayName("Existing ACTIVE subscription → trial NOT created")
    void testExistingActiveSubscription_noTrial() {
        Subscription existing = Subscription.builder()
                .id(1L)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(existing));

        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertFalse(result);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Existing TRIAL subscription → trial NOT created")
    void testExistingTrialSubscription_noTrial() {
        Subscription existing = Subscription.builder()
                .id(1L)
                .status(SubscriptionStatus.TRIAL)
                .build();
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByShopOwnerIdAndStatus(SHOP_OWNER_ID, SubscriptionStatus.TRIAL))
                .thenReturn(List.of(existing));

        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertFalse(result);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trial config inactive → trial NOT created")
    void testTrialInactive_noTrial() {
        SubscriptionTrialConfig config = createTrialConfig(14, 5, 7L, false);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertFalse(result);
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== Duplicate Trial Handling =====

    @Test
    @DisplayName("Concurrent trial creation → DataIntegrityViolationException swallowed")
    void testConcurrentTrialCreation_swallowed() {
        SubscriptionTrialConfig config = createTrialConfig(14, 5, 7L, true);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        SubscriptionPlan plan = createPublishedPlan(7L, "Starter", 10);
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(shopOwnerRepository.findById(SHOP_OWNER_ID)).thenReturn(Optional.of(createShopOwner(SHOP_OWNER_ID)));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate active subscription"));

        // Should NOT throw — the exception is swallowed
        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertFalse(result); // trial was not created by this call
    }

    // ===== Trial Plan Fallback =====

    @Test
    @DisplayName("Trial plan ID null → falls back to first published plan")
    void testTrialPlanNull_fallsBackToFirstPublished() {
        SubscriptionTrialConfig config = createTrialConfig(14, 5, null, true);
        when(trialConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(subscriptionRepository.findByShopOwnerIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        SubscriptionPlan fallbackPlan = createPublishedPlan(5L, "Basic", 5);
        when(subscriptionPlanRepository.findFirstByStatus(PlanStatus.PUBLISHED))
                .thenReturn(Optional.of(fallbackPlan));
        when(shopOwnerRepository.findById(SHOP_OWNER_ID)).thenReturn(Optional.of(createShopOwner(SHOP_OWNER_ID)));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        boolean result = service.startTrialIfNeeded(SHOP_OWNER_ID);

        assertTrue(result);
        verify(subscriptionRepository).save(argThat(s ->
                s.getPlan() != null && s.getPlan().getId().equals(5L)
        ));
    }
}
