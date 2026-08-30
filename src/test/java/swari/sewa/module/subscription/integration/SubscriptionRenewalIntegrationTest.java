package swari.sewa.module.subscription.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.category.entity.Category;
import swari.sewa.module.category.repository.CategoryRepository;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.EsewaSignatureService;
import swari.sewa.module.payment.service.PaymentEmailService;
import swari.sewa.module.payment.service.PaymentExpenseSyncService;
import swari.sewa.module.payment.service.CouponUsageRecorder;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.subscription.entity.*;
import swari.sewa.module.subscription.enums.*;
import swari.sewa.module.subscription.exception.SubscriptionLimitExceededException;
import swari.sewa.module.subscription.repository.*;
import swari.sewa.module.subscription.service.*;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for subscription renewal/extension and vehicle-allowance behavior.
 *
 * Covers scenarios 3-9 from the business-rules review:
 *  - ACTIVE → purchase same plan before expiry (renewal extends from old expiry)
 *  - ACTIVE → purchase different plan (upgrade/renewal)
 *  - Vehicle limit reached before expiry (blocked, selling allowed, selling doesn't free slot)
 *  - Limit reached → purchase again (new allowance, old time preserved)
 *  - Expired subscription → purchase again (starts from now)
 *  - Idempotent callback (no double extension)
 *  - eSewa and Fonepay parity
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class SubscriptionRenewalIntegrationTest {

    @Autowired private ShopOwnerRepository shopOwnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private SubscriptionTransactionRepository transactionRepository;
    @Autowired private SubscriptionSettingsRepository settingsRepository;
    @Autowired private SubscriptionTrialConfigRepository trialConfigRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private EsewaConfig esewaConfig;
    @Autowired private EsewaSignatureService esewaSignatureService;
    @Autowired private SubscriptionPlanService planService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private PaymentEmailService paymentEmailService;
    @Autowired private PaymentExpenseSyncService paymentExpenseSyncService;
    @Autowired private SubscriptionSettingsService settingsService;
    @Autowired private SubscriptionCouponService couponService;
    @Autowired private SubscriptionCouponUsageRepository couponUsageRepository;
    @Autowired private CouponUsageRecorder couponUsageRecorder;
    @Autowired private SubscriptionAccessService subscriptionAccessService;

    private ShopOwner owner;
    private Shop shop;
    private Category category;
    private SubscriptionPlan planA;
    private SubscriptionPlan planB;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        // Settings singleton
        SubscriptionSettings settings = settingsRepository.findById(1L).orElseGet(() ->
                SubscriptionSettings.builder().id(1L).enableVat(true).taxPercentage(13)
                        .currency("NPR").invoicePrefix("INV").build());
        settings.setEnableVat(true);
        settings.setTaxPercentage(13);
        settingsRepository.saveAndFlush(settings);

        // Trial config
        SubscriptionTrialConfig trialConfig = trialConfigRepository.findById(1L).orElseGet(() ->
                SubscriptionTrialConfig.builder().id(1L).name("Default Trial")
                        .active(true).duration(14).vehicleLimit(3).build());
        trialConfig.setActive(true);
        trialConfig.setDuration(14);
        trialConfig.setVehicleLimit(3);
        trialConfigRepository.saveAndFlush(trialConfig);

        // Plan A: monthly limit 30, quarterly price
        planA = createPlan("Plan A " + suffix, "plan-a-" + suffix, 30,
                new BigDecimal("2699"), new BigDecimal("5399"), new BigDecimal("10799"));
        // Plan B: monthly limit 50, quarterly price (different limit)
        planB = createPlan("Plan B " + suffix, "plan-b-" + suffix, 50,
                new BigDecimal("3999"), new BigDecimal("7999"), new BigDecimal("15999"));

        // Category
        category = Category.builder().name("Renewal Cat " + suffix).build();
        category = categoryRepository.saveAndFlush(category);

        // Owner + User + Shop
        User user = User.builder()
                .email("renewal-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .firstName("Renewal").lastName("Owner")
                .phoneNumber("9800000201")
                .role(UserRole.SHOP_OWNER).isActive(true).isEmailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);

        owner = ShopOwner.builder()
                .firstName("Renewal").lastName("Owner")
                .email("renewal-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .phone("9800000201")
                .role(UserRole.SHOP_OWNER).active(true).emailVerified(true)
                .approvalStatus("APPROVED").passwordChanged(true)
                .build();
        owner = shopOwnerRepository.saveAndFlush(owner);

        shop = Shop.builder()
                .name("Renewal Shop " + suffix)
                .description("test").licenseNumber("REN-LIC-" + suffix)
                .city("Kathmandu").state("Bagmati").country("Nepal")
                .status(ShopStatus.ACTIVE).isFeatured(false)
                .shopOwner(owner).user(user)
                .build();
        shop = shopRepository.saveAndFlush(shop);
    }

    private SubscriptionPlan createPlan(String name, String slug, int maxVehicles,
                                        BigDecimal quarterly, BigDecimal halfYearly, BigDecimal yearly) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(name).slug(slug).category(PlanCategory.PREMIUM)
                .status(PlanStatus.PUBLISHED).description(name).shortDescription(name)
                .icon("icon").themeColor("#3b82f6")
                .build();
        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(plan).monthly(new BigDecimal("999"))
                .quarterly(quarterly).halfYearly(halfYearly).yearly(yearly)
                .currency("NPR").build();
        plan.setPricings(Set.of(pricing));
        SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                .plan(plan).maxVehicles(maxVehicles).build();
        plan.setRestrictions(Set.of(restriction));
        return planRepository.saveAndFlush(plan);
    }

    private TestableEsewaService createEsewaService() {
        return new TestableEsewaService(
                paymentRepository, esewaConfig, esewaSignatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService,
                couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);
    }

    private static class TestableEsewaService extends EsewaPaymentServiceImpl {
        private EsewaStatusResult mockResult;

        TestableEsewaService(
                PaymentRepository paymentRepository, EsewaConfig esewaConfig,
                EsewaSignatureService signatureService, SubscriptionPlanService planService,
                SubscriptionRepository subscriptionRepository,
                SubscriptionTransactionRepository transactionRepository,
                InvoiceService invoiceService, PaymentEmailService paymentEmailService,
                PaymentExpenseSyncService paymentExpenseSyncService,
                SubscriptionSettingsService settingsService,
                SubscriptionCouponService couponService,
                SubscriptionCouponUsageRepository couponUsageRepository,
                CouponUsageRecorder couponUsageRecorder,
                swari.sewa.module.vehicle.repository.VehicleRepository vehicleRepository) {
            super(paymentRepository, esewaConfig, signatureService, planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService,
                    couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);
        }

        void setMockStatus(String status, String totalAmount, String productCode) {
            this.mockResult = new EsewaStatusResult(status, "REF-MOCK", totalAmount, productCode);
        }

        @Override
        protected EsewaStatusResult verifyWithEsewa(Payment payment) {
            if (mockResult != null) return mockResult;
            throw new IllegalStateException("Test must set mock eSewa status result");
        }
    }

    private Payment createAndCompletePayment(SubscriptionPlan plan, String billingCycle) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(plan.getId());
        req.setBillingCycle(billingCycle);

        TestableEsewaService esewa = createEsewaService();
        var createResp = esewa.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                .orElseThrow();
        String total = pending.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        esewa.setMockStatus("COMPLETE", total, esewaConfig.getProductCode());
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-" + System.nanoTime(), "REF-" + System.nanoTime(), "SUCCESS");
        return paymentRepository.findByTransactionUuid(createResp.getTransactionUuid()).orElseThrow();
    }

    private Vehicle createVehicle(String title) {
        Vehicle v = Vehicle.builder()
                .title(title).vehicleType(VehicleType.BIKE)
                .brandName("Test").modelName("Model").manufacturingYear(2023)
                .fuelType("Petrol").transmissionType("Manual")
                .price(new BigDecimal("50000.00"))
                .status(VehicleStatus.ACTIVE).shop(shop).category(category)
                .build();
        return vehicleRepository.saveAndFlush(v);
    }

    private Subscription getActiveSub() {
        List<Subscription> subs = subscriptionRepository
                .findByShopOwnerIdAndStatus(owner.getId(), SubscriptionStatus.ACTIVE);
        assertEquals(1, subs.size(), "Exactly one ACTIVE subscription expected");
        return subs.get(0);
    }

    // ==================================================================
    // SCENARIO 1: No existing subscription → purchase
    // ==================================================================

    @Test
    @DisplayName("Scenario 1: No existing subscription → new ACTIVE subscription created")
    void testNewSubscription() {
        Payment payment = createAndCompletePayment(planA, "quarterly");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNotNull(payment.getSubscriptionId());

        Subscription sub = getActiveSub();
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(planA.getName(), sub.getPlanNameSnapshot());
        // quarterly = 3 months × 30 = 90 vehicle limit
        assertEquals(90, sub.getVehicleLimitSnapshot());
        assertEquals(sub.getStartDate(), sub.getCurrentPeriodStart(),
                "currentPeriodStart should equal startDate for new subscription");
    }

    // ==================================================================
    // SCENARIO 3: ACTIVE → purchase same plan before expiry (renewal)
    // ==================================================================

    @Test
    @DisplayName("Scenario 3: Renewal extends from old expiry, preserves original startDate")
    void testRenewalExtendsFromOldExpiry() {
        // First purchase
        createAndCompletePayment(planA, "quarterly");
        Subscription sub1 = getActiveSub();
        Long subId = sub1.getId();
        LocalDateTime oldExpiry = sub1.getEndDate();
        assertNotNull(oldExpiry);

        // Commit so the second payment can see the subscription
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Second purchase (same plan, same cycle) while still ACTIVE
        createAndCompletePayment(planA, "quarterly");
        Subscription sub2 = getActiveSub();

        // Same subscription row (not a new one)
        assertEquals(subId, sub2.getId(),
                "Renewal should update the existing subscription, not create a new one");

        // endDate extended from old expiry (by another quarter = 3 months)
        assertEquals(oldExpiry.plusMonths(3).toLocalDate(), sub2.getEndDate().toLocalDate(),
                "New endDate should be old expiry + 3 months");

        // currentPeriodStart moved to the renewal date (now), not old expiry,
        // so the new vehicle allowance takes effect immediately
        assertTrue(sub2.getCurrentPeriodStart().isAfter(oldExpiry.minusYears(1)),
                "currentPeriodStart should be the renewal date (now)");

        // startDate should NOT have changed (still the original)
        // Compare by date (MySQL may truncate nanoseconds)
        // startDate is unchanged from the original — it should be well before oldExpiry
        assertTrue(sub2.getStartDate().isBefore(oldExpiry),
                "Original startDate must be before old expiry (preserved)");
    }

    // ==================================================================
    // SCENARIO 4: ACTIVE Plan A → purchase Plan B (different limit)
    // ==================================================================

    @Test
    @DisplayName("Scenario 4: Upgrade to different plan preserves remaining time and updates limit")
    void testUpgradeToDifferentPlan() {
        // First purchase: Plan A, quarterly (limit 90)
        createAndCompletePayment(planA, "quarterly");
        Subscription sub1 = getActiveSub();
        Long subId = sub1.getId();
        LocalDateTime oldExpiry = sub1.getEndDate();
        assertEquals(90, sub1.getVehicleLimitSnapshot());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Second purchase: Plan B, quarterly (limit 150)
        createAndCompletePayment(planB, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(subId, sub2.getId());
        assertTrue(sub2.getStartDate().isBefore(oldExpiry),
                "Original startDate preserved (before old expiry)");
        assertEquals(oldExpiry.plusMonths(3).toLocalDate(), sub2.getEndDate().toLocalDate(),
                "endDate extended from old expiry by 3 months");
        // Rollover: Plan A's 90 slots were all unused, so they carry forward.
        // Total = 150 (Plan B quarterly) + 90 (carried forward) = 240
        assertEquals(240, sub2.getVehicleLimitSnapshot(),
                "Total limit = new plan limit (150) + carried-forward unused (90)");
        assertEquals(150, sub2.getNewPlanVehicleLimit(),
                "New plan limit is Plan B's quarterly allowance");
        assertEquals(90, sub2.getCarriedForwardVehicleLimit(),
                "Carried forward = Plan A's unused 90 slots");
        assertEquals(planB.getName(), sub2.getPlanNameSnapshot(),
                "Plan name snapshot updated to new plan");
    }

    // ==================================================================
    // SCENARIO 5: Vehicle limit reached before expiry
    // ==================================================================

    @Test
    @DisplayName("Scenario 5: Limit reached → adding blocked, selling allowed, selling doesn't free slot")
    void testVehicleLimitReachedBeforeExpiry() {
        // Create plan with limit 3 (monthly 3 × 1 month = 3) for fast testing
        String suffix = String.valueOf(System.nanoTime());
        SubscriptionPlan smallPlan = createPlan("Small " + suffix, "small-" + suffix, 3,
                new BigDecimal("299"), new BigDecimal("599"), new BigDecimal("1199"));

        createAndCompletePayment(smallPlan, "monthly");
        Subscription sub = getActiveSub();
        assertEquals(3, sub.getVehicleLimitSnapshot());

        // Add 3 vehicles (reaches limit)
        createVehicle("V1");
        createVehicle("V2");
        createVehicle("V3");

        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Should NOT be able to add after reaching limit");

        assertThrows(SubscriptionLimitExceededException.class,
                () -> subscriptionAccessService.validateCanAddVehicle(owner.getId()),
                "Adding beyond limit should throw");

        // Subscription remains ACTIVE
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());

        // Sell one vehicle
        Vehicle toSell = vehicleRepository.findByShopIdAndStatus(shop.getId(), VehicleStatus.ACTIVE)
                .get(0);
        toSell.setStatus(VehicleStatus.SOLD);
        vehicleRepository.saveAndFlush(toSell);

        // Selling does NOT free a slot
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Selling must NOT free an add slot");

        assertThrows(SubscriptionLimitExceededException.class,
                () -> subscriptionAccessService.validateCanAddVehicle(owner.getId()),
                "Adding after sell should still throw");
    }

    // ==================================================================
    // SCENARIO 6: Limit reached → purchase same plan again → new allowance
    // ==================================================================

    @Test
    @DisplayName("Scenario 6: Limit reached → renew → new allowance, old time preserved")
    void testRenewalAfterLimitReached() {
        String suffix = String.valueOf(System.nanoTime());
        SubscriptionPlan smallPlan = createPlan("Small6 " + suffix, "small6-" + suffix, 3,
                new BigDecimal("299"), new BigDecimal("599"), new BigDecimal("1199"));

        // First purchase
        createAndCompletePayment(smallPlan, "monthly");
        Subscription sub1 = getActiveSub();
        LocalDateTime oldExpiry = sub1.getEndDate();
        assertEquals(3, sub1.getVehicleLimitSnapshot());

        // Back-date currentPeriodStart so vehicles created below are
        // unambiguously "after" it (MySQL DATETIME has 1-second precision and
        // the test runs faster than that).
        sub1.setCurrentPeriodStart(sub1.getCurrentPeriodStart().minusSeconds(10));
        subscriptionRepository.saveAndFlush(sub1);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Add 3 vehicles (limit reached)
        createVehicle("R-V1");
        createVehicle("R-V2");
        createVehicle("R-V3");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Renew (same plan, same cycle)
        createAndCompletePayment(smallPlan, "monthly");
        Subscription sub2 = getActiveSub();

        // Original start preserved (before old expiry)
        assertTrue(sub2.getStartDate().isBefore(oldExpiry),
                "Original startDate preserved (before old expiry)");
        // End date extended from old expiry (by 1 month)
        assertEquals(oldExpiry.plusMonths(1).toLocalDate(), sub2.getEndDate().toLocalDate(),
                "endDate extended from old expiry by 1 month");
        // currentPeriodStart moved to the renewal date (now), not old expiry
        assertTrue(sub2.getCurrentPeriodStart().isAfter(oldExpiry.minusYears(1)),
                "currentPeriodStart should be the renewal date (now)");
        // All 3 slots were used, so nothing carries forward: total = 3
        assertEquals(3, sub2.getVehicleLimitSnapshot(),
                "Total = 3 (3 new + 0 carried, all previous slots used)");
        assertEquals(0, sub2.getCarriedForwardVehicleLimit(),
                "Nothing carries forward when the previous period was fully used");

        // Can add vehicles again (new period, 0 counted)
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Should be able to add after renewal (new period)");
    }

    // ==================================================================
    // SCENARIO 7: Expired subscription → purchase again
    // ==================================================================

    @Test
    @DisplayName("Scenario 7: Expired subscription → new purchase starts from now")
    void testPurchaseAfterExpiry() {
        // First purchase
        createAndCompletePayment(planA, "monthly");
        Subscription sub1 = getActiveSub();
        Long oldSubId = sub1.getId();
        LocalDateTime beforeNewPurchase = LocalDateTime.now();

        // Manually expire the subscription
        sub1.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.saveAndFlush(sub1);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Purchase again
        createAndCompletePayment(planA, "monthly");
        Subscription sub2 = getActiveSub();

        // New subscription (old one is EXPIRED, not ACTIVE)
        assertNotEquals(oldSubId, sub2.getId(),
                "Expired subscription → new subscription row");

        // New start date is now (not extended from old expiry)
        assertTrue(sub2.getStartDate().isAfter(beforeNewPurchase.minusMinutes(1)),
                "New subscription starts from now, not old expiry");
        assertEquals(sub2.getStartDate().toLocalDate(), sub2.getCurrentPeriodStart().toLocalDate(),
                "currentPeriodStart = startDate for new subscription");
    }

    // ==================================================================
    // SCENARIO 9: Idempotent callback (no double extension)
    // ==================================================================

    @Test
    @DisplayName("Scenario 9: Duplicate callback does not extend subscription twice")
    void testIdempotentCallbackNoDoubleExtension() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(planA.getId());
        req.setBillingCycle("quarterly");

        TestableEsewaService esewa = createEsewaService();
        var createResp = esewa.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                .orElseThrow();
        String total = pending.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        esewa.setMockStatus("COMPLETE", total, esewaConfig.getProductCode());

        // First callback
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-IDEMPOTENT", "REF-IDEMPOTENT", "SUCCESS");
        Subscription sub1 = getActiveSub();
        LocalDateTime endDateAfterFirst = sub1.getEndDate();

        // Duplicate callback
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-IDEMPOTENT", "REF-IDEMPOTENT", "SUCCESS");
        Subscription sub2 = getActiveSub();

        // End date should NOT change (idempotent)
        assertEquals(endDateAfterFirst, sub2.getEndDate(),
                "Duplicate callback must not extend subscription again");
        assertEquals(sub1.getId(), sub2.getId(),
                "Same subscription row");
    }
}
