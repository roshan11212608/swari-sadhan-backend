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
import swari.sewa.common.enums.VehicleType;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.category.entity.Category;
import swari.sewa.module.category.repository.CategoryRepository;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.controller.PaymentController;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.*;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.payment.service.impl.FonepayPaymentServiceImpl;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.subscription.entity.*;
import swari.sewa.module.subscription.enums.*;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the previous-plan identification feature.
 *
 * Verifies that the Subscription Dashboard correctly shows the immediately
 * preceding subscription period using immutable Payment-level snapshots,
 * not the reused/overwritten Subscription entity.
 *
 * Scenarios tested:
 * 1. Plan A → Plan B upgrade
 * 2. Plan A → Plan A renewal (same plan, different period)
 * 3. Plan A → Plan B → Plan C (chain)
 * 4. Multiple renewals (A → A → A)
 * 5. Failed/pending payments excluded
 * 6. Duplicate callback idempotency
 * 7. Expired → new purchase
 * 8. Different billing cycles (Monthly → Quarterly → Yearly)
 * 9. Historical immutability (previous plan doesn't change after future renewals)
 * 10. Fonepay parity
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class PreviousPlanIdentificationTest {

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

    @Autowired private FonepayConfig fonepayConfig;
    @Autowired private FonepaySignatureService fonepaySignatureService;

    private ShopOwner owner;
    private Shop shop;
    private Category category;
    private SubscriptionPlan planA, planB, planC;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        SubscriptionSettings settings = settingsRepository.findById(1L).orElseGet(() ->
                SubscriptionSettings.builder().id(1L).enableVat(true).taxPercentage(13)
                        .currency("NPR").invoicePrefix("INV").build());
        settings.setEnableVat(true);
        settings.setTaxPercentage(13);
        settingsRepository.saveAndFlush(settings);

        SubscriptionTrialConfig trialConfig = trialConfigRepository.findById(1L).orElseGet(() ->
                SubscriptionTrialConfig.builder().id(1L).name("Default Trial")
                        .active(true).duration(14).vehicleLimit(3).build());
        trialConfig.setActive(true);
        trialConfig.setDuration(14);
        trialConfig.setVehicleLimit(3);
        trialConfigRepository.saveAndFlush(trialConfig);

        planA = createPlan("PlanA " + suffix, "plana-" + suffix, 1);
        planB = createPlan("PlanB " + suffix, "planb-" + suffix, 5);
        planC = createPlan("PlanC " + suffix, "planc-" + suffix, 10);

        category = Category.builder().name("PrevCat " + suffix).build();
        category = categoryRepository.saveAndFlush(category);

        User user = User.builder()
                .email("prev-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .firstName("Prev").lastName("Owner")
                .phoneNumber("9800000401")
                .role(UserRole.SHOP_OWNER).isActive(true).isEmailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);

        owner = ShopOwner.builder()
                .firstName("Prev").lastName("Owner")
                .email("prev-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .phone("9800000401")
                .role(UserRole.SHOP_OWNER).active(true).emailVerified(true)
                .approvalStatus("APPROVED").passwordChanged(true)
                .build();
        owner = shopOwnerRepository.saveAndFlush(owner);

        shop = Shop.builder()
                .name("Prev Shop " + suffix)
                .description("test").licenseNumber("PREV-LIC-" + suffix)
                .city("Kathmandu").state("Bagmati").country("Nepal")
                .status(ShopStatus.ACTIVE).isFeatured(false)
                .shopOwner(owner).user(user)
                .build();
        shop = shopRepository.saveAndFlush(shop);
    }

    private SubscriptionPlan createPlan(String name, String slug, int maxVehicles) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(name).slug(slug).category(PlanCategory.PREMIUM)
                .status(PlanStatus.PUBLISHED).description(name).shortDescription(name)
                .icon("icon").themeColor("#3b82f6")
                .build();
        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(plan).monthly(new BigDecimal("99"))
                .quarterly(new BigDecimal("299")).halfYearly(new BigDecimal("599"))
                .yearly(new BigDecimal("1199"))
                .currency("NPR").build();
        plan.setPricings(Set.of(pricing));
        SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                .plan(plan).maxVehicles(maxVehicles).build();
        plan.setRestrictions(Set.of(restriction));
        return planRepository.saveAndFlush(plan);
    }

    // === eSewa helpers ===
    private TestableEsewaService createEsewaService() {
        return new TestableEsewaService(
                paymentRepository, esewaConfig, esewaSignatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService,
                couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);
    }

    private static class TestableEsewaService extends EsewaPaymentServiceImpl {
        private EsewaStatusResult mockResult;
        TestableEsewaService(PaymentRepository p, EsewaConfig c, EsewaSignatureService s,
                             SubscriptionPlanService ps, SubscriptionRepository sr,
                             SubscriptionTransactionRepository tr, InvoiceService is,
                             PaymentEmailService pe, PaymentExpenseSyncService es,
                             SubscriptionSettingsService ss, SubscriptionCouponService cs,
                             SubscriptionCouponUsageRepository cu, CouponUsageRecorder cr,
                             VehicleRepository vr) {
            super(p, c, s, ps, sr, tr, is, pe, es, ss, cs, cu, cr, vr);
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

    private Payment completeEsewaPayment(SubscriptionPlan plan, String billingCycle) {
        TestableEsewaService esewa = createEsewaService();
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(plan.getId());
        req.setBillingCycle(billingCycle);
        var createResp = esewa.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                .orElseThrow();
        String total = pending.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        esewa.setMockStatus("COMPLETE", total, esewaConfig.getProductCode());
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-" + System.nanoTime(), "REF-" + System.nanoTime(), "SUCCESS");
        return paymentRepository.findByTransactionUuid(createResp.getTransactionUuid()).orElseThrow();
    }

    // === Fonepay helpers ===
    private TestableFonepayService createFonepayService() {
        return new TestableFonepayService(
                paymentRepository, fonepayConfig, fonepaySignatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService, vehicleRepository);
    }

    private static class TestableFonepayService extends FonepayPaymentServiceImpl {
        TestableFonepayService(PaymentRepository p, FonepayConfig c, FonepaySignatureService s,
                               SubscriptionPlanService ps, SubscriptionRepository sr,
                               SubscriptionTransactionRepository tr, InvoiceService is,
                               PaymentEmailService pe, PaymentExpenseSyncService es,
                               SubscriptionSettingsService ss, VehicleRepository vr) {
            super(p, c, s, ps, sr, tr, is, pe, es, ss, vr);
        }
    }

    private Payment completeFonepayPayment(SubscriptionPlan plan, String billingCycle) {
        TestableFonepayService fonepay = createFonepayService();
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(plan.getId());
        req.setBillingCycle(billingCycle);
        fonepay.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.PENDING).get(0);
        String prn = pending.getTransactionUuid();
        String total = pending.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String pid = fonepayConfig.getMerchantCodePid();
        String uid = "UID-" + System.nanoTime();
        String bid = "BID-" + System.nanoTime();
        String verifyString = pid + "," + total + "," + prn + "," + bid + "," + uid;
        String dv = fonepaySignatureService.generateSignature(verifyString, fonepayConfig.getMerchantSecretKey());
        fonepay.handleCallback(prn, pid, "success", total, uid, bid, dv);
        return paymentRepository.findByTransactionUuid(prn).orElseThrow();
    }

    // === Common helpers ===
    private void commitAndStart() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    private Subscription getActiveSub() {
        List<Subscription> subs = subscriptionRepository
                .findByShopOwnerIdAndStatus(owner.getId(), SubscriptionStatus.ACTIVE);
        assertEquals(1, subs.size(), "Exactly one ACTIVE subscription expected");
        return subs.get(0);
    }

    /**
     * Compare two LocalDateTime values ignoring sub-second precision
     * (MySQL DATETIME truncates nanoseconds). Allows ±1 second tolerance
     * because the in-memory value may have nanoseconds that get truncated
     * differently when reloaded from the DB.
     */
    private void assertDatesMatch(String msg, LocalDateTime expected, LocalDateTime actual) {
        assertNotNull(actual, msg + " — actual is null");
        assertNotNull(expected, msg + " — expected is null");
        assertEquals(expected.toLocalDate(), actual.toLocalDate(),
                msg + " — date mismatch");
        long expectedSeconds = expected.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toSecondOfDay();
        long actualSeconds = actual.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toSecondOfDay();
        assertTrue(Math.abs(expectedSeconds - actualSeconds) <= 1,
                msg + " — time mismatch: expected " + expected.toLocalTime() + " but was " + actual.toLocalTime());
    }

    /**
     * Get the previous plan info by examining the second-most-recent SUCCESS payment.
     * This mirrors what the controller does.
     */
    private Payment getPreviousPayment() {
        var payments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.SUCCESS);
        assertTrue(payments.size() >= 2, "Need at least 2 successful payments for previous plan");
        return payments.get(1);
    }

    private Payment getLatestPayment() {
        var payments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.SUCCESS);
        assertFalse(payments.isEmpty(), "Need at least 1 successful payment");
        return payments.get(0);
    }

    // ==================================================================
    // TEST 1: Plan A → Plan B upgrade
    // ==================================================================
    @Test
    @DisplayName("TEST 1: Plan A → Plan B upgrade → previous = Plan A")
    void testUpgradePlanAtoB() {
        // Purchase Plan A
        Payment p1 = completeEsewaPayment(planA, "monthly");
        assertEquals("PlanA " + planA.getSlug().split("-")[1], p1.getPlanNameSnapshot());
        assertNotNull(p1.getSubscriptionStartDateSnapshot());
        assertNotNull(p1.getSubscriptionEndDateSnapshot());
        LocalDateTime p1Start = p1.getSubscriptionStartDateSnapshot();
        LocalDateTime p1End = p1.getSubscriptionEndDateSnapshot();

        commitAndStart();

        // Upgrade to Plan B
        Payment p2 = completeEsewaPayment(planB, "monthly");
        assertEquals(planB.getName(), p2.getPlanNameSnapshot());

        // Previous payment should be Plan A with its original dates
        Payment prev = getPreviousPayment();
        assertEquals(p1.getTransactionUuid(), prev.getTransactionUuid());
        assertEquals(planA.getName(), prev.getPlanNameSnapshot(),
                "Previous plan name must be Plan A (from Payment snapshot, not overwritten Subscription)");
        assertDatesMatch("Previous plan start date", p1Start, prev.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Previous plan end date", p1End, prev.getSubscriptionEndDateSnapshot());
    }

    // ==================================================================
    // TEST 2: Plan A → Plan A renewal (same plan, different period)
    // ==================================================================
    @Test
    @DisplayName("TEST 2: Plan A → Plan A renewal → previous = previous Plan A period")
    void testSamePlanRenewal() {
        Payment p1 = completeEsewaPayment(planA, "monthly");
        LocalDateTime p1Start = p1.getSubscriptionStartDateSnapshot();
        LocalDateTime p1End = p1.getSubscriptionEndDateSnapshot();

        commitAndStart();

        Payment p2 = completeEsewaPayment(planA, "monthly");
        assertEquals(planA.getName(), p2.getPlanNameSnapshot());

        // Previous payment is the first Plan A purchase
        Payment prev = getPreviousPayment();
        assertEquals(planA.getName(), prev.getPlanNameSnapshot(),
                "Previous plan name should be Plan A (same plan, different period)");
        assertDatesMatch("Previous period start", p1Start, prev.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Previous period end", p1End, prev.getSubscriptionEndDateSnapshot());
        assertNotEquals(p2.getSubscriptionStartDateSnapshot(), prev.getSubscriptionStartDateSnapshot(),
                "Current and previous periods must differ");
    }

    // ==================================================================
    // TEST 3: Plan A → Plan B → Plan C (chain)
    // ==================================================================
    @Test
    @DisplayName("TEST 3: Plan A → Plan B → Plan C → previous = Plan B (NOT Plan A)")
    void testChainUpgrade() {
        completeEsewaPayment(planA, "monthly");
        commitAndStart();

        completeEsewaPayment(planB, "monthly");
        commitAndStart();

        completeEsewaPayment(planC, "monthly");

        // Previous should be Plan B, not Plan A
        Payment prev = getPreviousPayment();
        assertEquals(planB.getName(), prev.getPlanNameSnapshot(),
                "Previous plan must be Plan B (immediately preceding), NOT Plan A");
    }

    // ==================================================================
    // TEST 4: Multiple renewals (A → A → A)
    // ==================================================================
    @Test
    @DisplayName("TEST 4: Multiple renewals → previous = immediately preceding period")
    void testMultipleRenewalsWithCommits() {
        completeEsewaPayment(planA, "monthly");
        LocalDateTime p1End = getLatestPayment().getSubscriptionEndDateSnapshot();
        commitAndStart();

        completeEsewaPayment(planA, "monthly");
        LocalDateTime p2End = getLatestPayment().getSubscriptionEndDateSnapshot();
        commitAndStart();

        completeEsewaPayment(planA, "monthly");

        // Previous = second purchase (immediately preceding the third)
        Payment prev = getPreviousPayment();
        assertEquals(planA.getName(), prev.getPlanNameSnapshot());
        assertDatesMatch("Previous must be 2nd period end", p2End, prev.getSubscriptionEndDateSnapshot());
    }

    // ==================================================================
    // TEST 5: Failed/pending payments excluded
    // ==================================================================
    @Test
    @DisplayName("TEST 5: Failed/pending payments do NOT become previous plans")
    void testFailedPaymentExcluded() {
        completeEsewaPayment(planA, "monthly");
        commitAndStart();

        // Create a PENDING payment (never completed) for Plan B
        TestableEsewaService esewa = createEsewaService();
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(planB.getId());
        req.setBillingCycle("monthly");
        esewa.createPayment(req, owner.getId());
        // Don't complete it — leave it PENDING

        commitAndStart();

        // Complete a different payment for Plan C
        completeEsewaPayment(planC, "monthly");

        // Previous should be Plan A (the PENDING Plan B is excluded)
        Payment prev = getPreviousPayment();
        assertEquals(planA.getName(), prev.getPlanNameSnapshot(),
                "Previous must be Plan A — PENDING Plan B payment must be excluded");
    }

    // ==================================================================
    // TEST 6: Duplicate callback idempotency
    // ==================================================================
    @Test
    @DisplayName("TEST 6: Duplicate callback → no duplicate historical plan")
    void testDuplicateCallbackIdempotency() {
        TestableEsewaService esewa = createEsewaService();
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(planA.getId());
        req.setBillingCycle("monthly");
        var createResp = esewa.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                .orElseThrow();
        String total = pending.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        esewa.setMockStatus("COMPLETE", total, esewaConfig.getProductCode());

        // First callback
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-DUP", "REF-DUP", "SUCCESS");
        // Duplicate callback
        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-DUP", "REF-DUP", "SUCCESS");

        // Only 1 SUCCESS payment should exist
        var successPayments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.SUCCESS);
        assertEquals(1, successPayments.size(),
                "Duplicate callback must NOT create a second SUCCESS payment");
    }

    // ==================================================================
    // TEST 7: Expired → new purchase
    // ==================================================================
    @Test
    @DisplayName("TEST 7: Expired subscription → new purchase → previous = expired plan")
    void testExpiredThenNewPurchase() {
        Payment p1 = completeEsewaPayment(planA, "monthly");
        LocalDateTime p1Start = p1.getSubscriptionStartDateSnapshot();
        LocalDateTime p1End = p1.getSubscriptionEndDateSnapshot();

        // Expire the subscription
        Subscription sub = getActiveSub();
        sub.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.saveAndFlush(sub);
        commitAndStart();

        // Purchase Plan B
        Payment p2 = completeEsewaPayment(planB, "monthly");

        // Previous should be Plan A
        Payment prev = getPreviousPayment();
        assertEquals(planA.getName(), prev.getPlanNameSnapshot(),
                "Previous must be Plan A (the expired subscription)");
        assertDatesMatch("Previous start date", p1Start, prev.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Previous end date", p1End, prev.getSubscriptionEndDateSnapshot());
    }

    // ==================================================================
    // TEST 8: Different billing cycles (Monthly → Quarterly → Yearly)
    // ==================================================================
    @Test
    @DisplayName("TEST 8: Monthly → Quarterly → Yearly → previous = Quarterly")
    void testDifferentBillingCycles() {
        completeEsewaPayment(planA, "monthly");
        commitAndStart();

        Payment p2 = completeEsewaPayment(planB, "quarterly");
        assertEquals("quarterly", p2.getBillingCycle());
        LocalDateTime p2Start = p2.getSubscriptionStartDateSnapshot();
        LocalDateTime p2End = p2.getSubscriptionEndDateSnapshot();
        commitAndStart();

        completeEsewaPayment(planC, "yearly");

        // Previous = Quarterly Plan B
        Payment prev = getPreviousPayment();
        assertEquals(planB.getName(), prev.getPlanNameSnapshot(),
                "Previous must be Plan B (quarterly)");
        assertEquals("quarterly", prev.getBillingCycle(),
                "Previous billing cycle must be quarterly");
        assertDatesMatch("Previous quarterly start", p2Start, prev.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Previous quarterly end", p2End, prev.getSubscriptionEndDateSnapshot());
    }

    // ==================================================================
    // TEST 9: Historical immutability — previous doesn't change after future renewals
    // ==================================================================
    @Test
    @DisplayName("TEST 9: Historical immutability — previous plan doesn't change after future renewals")
    void testHistoricalImmutability() {
        // Plan A purchase
        Payment p1 = completeEsewaPayment(planA, "monthly");
        String p1PlanName = p1.getPlanNameSnapshot();
        LocalDateTime p1Start = p1.getSubscriptionStartDateSnapshot();
        LocalDateTime p1End = p1.getSubscriptionEndDateSnapshot();
        commitAndStart();

        // Upgrade to Plan B
        completeEsewaPayment(planB, "monthly");
        commitAndStart();

        // At this point: current = Plan B, previous = Plan A
        Payment prev1 = getPreviousPayment();
        assertEquals(planA.getName(), prev1.getPlanNameSnapshot());
        assertDatesMatch("Prev1 start", p1Start, prev1.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Prev1 end", p1End, prev1.getSubscriptionEndDateSnapshot());

        // Renew Plan B
        completeEsewaPayment(planB, "monthly");
        commitAndStart();

        // Now: current = Plan B renewal, previous = Plan B (first period)
        // The previous plan must NOT suddenly show Plan A
        Payment prev2 = getPreviousPayment();
        assertEquals(planB.getName(), prev2.getPlanNameSnapshot(),
                "After renewing Plan B, previous must be Plan B's first period, NOT Plan A");
        assertNotEquals(planA.getName(), prev2.getPlanNameSnapshot(),
                "Previous must NOT regress to Plan A after Plan B renewal");

        // The very first Plan A payment's snapshot must still be intact
        var allPayments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.SUCCESS);
        Payment oldestPayment = allPayments.get(allPayments.size() - 1);
        assertEquals(p1PlanName, oldestPayment.getPlanNameSnapshot(),
                "Oldest payment snapshot must remain immutable");
        assertDatesMatch("Oldest start", p1Start, oldestPayment.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Oldest end", p1End, oldestPayment.getSubscriptionEndDateSnapshot());
    }

    // ==================================================================
    // TEST 10: Fonepay parity — same behavior as eSewa
    // ==================================================================
    @Test
    @DisplayName("TEST 10 (Fonepay): Plan A → Plan B → previous = Plan A with correct snapshots")
    void testFonepayParity() {
        Payment p1 = completeFonepayPayment(planA, "monthly");
        assertNotNull(p1.getPlanNameSnapshot(), "Fonepay: planNameSnapshot must be set");
        assertEquals(planA.getName(), p1.getPlanNameSnapshot());
        LocalDateTime p1Start = p1.getSubscriptionStartDateSnapshot();
        LocalDateTime p1End = p1.getSubscriptionEndDateSnapshot();
        assertNotNull(p1Start, "Fonepay: subscriptionStartDateSnapshot must be set");
        assertNotNull(p1End, "Fonepay: subscriptionEndDateSnapshot must be set");

        commitAndStart();

        Payment p2 = completeFonepayPayment(planB, "monthly");
        assertEquals(planB.getName(), p2.getPlanNameSnapshot());

        Payment prev = getPreviousPayment();
        assertEquals(planA.getName(), prev.getPlanNameSnapshot(),
                "Fonepay: previous plan must be Plan A");
        assertDatesMatch("Fonepay: previous start", p1Start, prev.getSubscriptionStartDateSnapshot());
        assertDatesMatch("Fonepay: previous end", p1End, prev.getSubscriptionEndDateSnapshot());
    }
}
