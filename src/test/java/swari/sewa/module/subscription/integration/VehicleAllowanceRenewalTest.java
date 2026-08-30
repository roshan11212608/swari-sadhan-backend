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
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.*;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.payment.service.impl.FonepayPaymentServiceImpl;
import swari.sewa.module.payment.service.FonepaySignatureService;
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
 * Integration tests verifying vehicle allowance carry-forward (rollover) across renewals.
 *
 * Business rules verified:
 * - Unused vehicle allowance carries forward on renewal
 * - CURRENT ALLOWANCE = NEW PLAN ALLOWANCE + PREVIOUS PERIOD UNUSED
 * - Selling does NOT free a slot
 * - Multiple renewals accumulate unused correctly
 * - Idempotent callbacks do not double the rollover
 * - Both eSewa and Fonepay behave identically
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class VehicleAllowanceRenewalTest {

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

    // Fonepay dependencies
    @Autowired private FonepayConfig fonepayConfig;
    @Autowired private FonepaySignatureService fonepaySignatureService;

    private ShopOwner owner;
    private Shop shop;
    private Category category;
    private SubscriptionPlan planLimit1;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        // Settings
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

        // Plan: monthly limit = 1, quarterly price → quarterly allowance = 1×3 = 3
        planLimit1 = createPlan("AllowTest " + suffix, "allow-" + suffix, 1,
                new BigDecimal("299"), new BigDecimal("599"), new BigDecimal("1199"));

        // Category
        category = Category.builder().name("AllowCat " + suffix).build();
        category = categoryRepository.saveAndFlush(category);

        // Owner + User + Shop
        User user = User.builder()
                .email("allow-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .firstName("Allow").lastName("Owner")
                .phoneNumber("9800000301")
                .role(UserRole.SHOP_OWNER).isActive(true).isEmailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);

        owner = ShopOwner.builder()
                .firstName("Allow").lastName("Owner")
                .email("allow-" + suffix + "@swari.com")
                .password(passwordEncoder.encode("Test123!"))
                .phone("9800000301")
                .role(UserRole.SHOP_OWNER).active(true).emailVerified(true)
                .approvalStatus("APPROVED").passwordChanged(true)
                .build();
        owner = shopOwnerRepository.saveAndFlush(owner);

        shop = Shop.builder()
                .name("Allow Shop " + suffix)
                .description("test").licenseNumber("ALLOW-LIC-" + suffix)
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
                .plan(plan).monthly(new BigDecimal("99"))
                .quarterly(quarterly).halfYearly(halfYearly).yearly(yearly)
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
        // Fonepay createPayment returns a URL string, not a response object.
        // Find the latest PENDING payment for this owner (highest ID = most recent).
        List<Payment> pendingPayments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                owner.getId(), PaymentStatus.PENDING);
        Payment pending = pendingPayments.get(0);
        String prn = pending.getTransactionUuid();
        String total = pending.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String pid = fonepayConfig.getMerchantCodePid();
        String uid = "UID-" + System.nanoTime();
        String bid = "BID-" + System.nanoTime();
        // Generate a valid HMAC-SHA512 signature
        String verifyString = pid + "," + total + "," + prn + "," + bid + "," + uid;
        String dv = fonepaySignatureService.generateSignature(verifyString, fonepayConfig.getMerchantSecretKey());
        fonepay.handleCallback(prn, pid, "success", total, uid, bid, dv);
        return paymentRepository.findByTransactionUuid(prn).orElseThrow();
    }

    // === Common helpers ===
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

    private void commitAndStart() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    /**
     * Back-date the active subscription's currentPeriodStart so that vehicles
     * created afterwards are unambiguously "after" the period start.
     *
     * Without this, the test runs so fast that the subscription's
     * currentPeriodStart and the vehicles' createdAt land in the same second.
     * MySQL DATETIME has 1-second precision, so the `createdAt >= periodStart`
     * comparison becomes ambiguous and the rollover count is unreliable.
     * Production doesn't have this problem because real usage spans minutes/days.
     */
    private void backdatePeriodStart(int secondsBack) {
        Subscription sub = getActiveSub();
        sub.setCurrentPeriodStart(sub.getCurrentPeriodStart().minusSeconds(secondsBack));
        subscriptionRepository.saveAndFlush(sub);
    }

    // ==================================================================
    // TEST 1: 0/3 → renewal → total allowance = 6 (3 new + 3 carried forward)
    // ==================================================================
    @Test
    @DisplayName("TEST 1 (eSewa): 0/3 → renewal → total allowance = 6")
    void testRenewalZeroUsed_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub1 = getActiveSub();
        assertEquals(3, sub1.getVehicleLimitSnapshot());
        assertEquals(3, sub1.getNewPlanVehicleLimit());
        assertEquals(0, sub1.getCarriedForwardVehicleLimit());
        LocalDateTime oldExpiry = sub1.getEndDate();

        // Add 0 vehicles, then renew
        commitAndStart();
        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        // totalLimit = 3 (new) + 3 (carried forward) = 6
        assertEquals(6, sub2.getVehicleLimitSnapshot(),
                "Total allowance = 6 (3 new + 3 carried forward)");
        assertEquals(3, sub2.getNewPlanVehicleLimit(), "New plan limit = 3");
        assertEquals(3, sub2.getCarriedForwardVehicleLimit(), "Carried forward = 3");
        assertEquals(oldExpiry.plusMonths(3).toLocalDate(), sub2.getEndDate().toLocalDate());

        // Can add 6 vehicles
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
        for (int i = 1; i <= 5; i++) createVehicle("T1-V" + i);
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()), "5/6 should allow 1 more");
        createVehicle("T1-V6");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()), "6/6 must be blocked");
    }

    // ==================================================================
    // TEST 2: 3/3 → renewal → total allowance = 3 (0 carried forward)
    // ==================================================================
    @Test
    @DisplayName("TEST 2 (eSewa): 3/3 → renewal → total allowance = 3")
    void testRenewalAllUsed_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        backdatePeriodStart(10);
        commitAndStart();

        createVehicle("T2-V1"); createVehicle("T2-V2"); createVehicle("T2-V3");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()));
        commitAndStart();

        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(3, sub2.getVehicleLimitSnapshot(), "Total = 3 (3 new + 0 carried)");
        assertEquals(0, sub2.getCarriedForwardVehicleLimit(), "Carried forward = 0 (all used)");
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Old vehicles don't count against new period");
    }

    // ==================================================================
    // TEST 3: 1/3 → renewal → total allowance = 5 (3 new + 2 carried)
    // ==================================================================
    @Test
    @DisplayName("TEST 3 (eSewa): 1/3 → renewal → total allowance = 5")
    void testRenewalPartialUsed_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        backdatePeriodStart(10);
        commitAndStart();

        createVehicle("T3-V1");
        commitAndStart();

        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(5, sub2.getVehicleLimitSnapshot(), "Total = 5 (3 new + 2 carried)");
        assertEquals(3, sub2.getNewPlanVehicleLimit());
        assertEquals(2, sub2.getCarriedForwardVehicleLimit());
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
    }

    // ==================================================================
    // TEST 4: 2/3 → renewal → total allowance = 4
    // ==================================================================
    @Test
    @DisplayName("TEST 4 (eSewa): 2/3 → renewal → total allowance = 4")
    void testRenewalTwoUsed_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        backdatePeriodStart(10);
        commitAndStart();

        createVehicle("T4-V1"); createVehicle("T4-V2");
        commitAndStart();

        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(4, sub2.getVehicleLimitSnapshot(), "Total = 4 (3 new + 1 carried)");
        assertEquals(1, sub2.getCarriedForwardVehicleLimit());
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
    }

    // ==================================================================
    // TEST 5: Multiple renewals with rollover accumulation
    // ==================================================================
    @Test
    @DisplayName("TEST 5 (eSewa): Multiple renewals → rollover accumulates correctly")
    void testMultipleRenewals_esewa() {
        // Period 1: 0/3 used
        completeEsewaPayment(planLimit1, "quarterly");
        commitAndStart();

        // Renew 1: 3 new + 3 carried = 6
        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();
        assertEquals(6, sub2.getVehicleLimitSnapshot(), "After 1st renewal: 6");
        assertEquals(3, sub2.getCarriedForwardVehicleLimit());
        backdatePeriodStart(10);
        commitAndStart();

        // Use 2 vehicles in period 2
        createVehicle("T5-V1"); createVehicle("T5-V2");
        commitAndStart();

        // Renew 2: 3 new + (6-2)=4 carried → total=7
        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub3 = getActiveSub();
        assertEquals(7, sub3.getVehicleLimitSnapshot(), "After 2nd renewal: 7 (3 new + 4 carried)");
        assertEquals(4, sub3.getCarriedForwardVehicleLimit());
    }

    // ==================================================================
    // TEST 6: Selling does NOT free a slot
    // ==================================================================
    @Test
    @DisplayName("TEST 6 (eSewa): Selling does NOT free a slot")
    void testSellingDoesNotFreeSlot_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        createVehicle("T6-V1"); createVehicle("T6-V2"); createVehicle("T6-V3");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()));

        Vehicle toSell = vehicleRepository.findByShopIdAndStatus(shop.getId(), VehicleStatus.ACTIVE).get(0);
        toSell.setStatus(VehicleStatus.SOLD);
        vehicleRepository.saveAndFlush(toSell);

        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Selling must NOT free a slot");
        var usage = subscriptionAccessService.getVehicleUsage(owner.getId());
        assertEquals(3, usage.getCurrentVehicleCount(), "SOLD still counted");
    }

    // ==================================================================
    // TEST 7: Expired → new purchase → NO carry forward (different subscription)
    // ==================================================================
    @Test
    @DisplayName("TEST 7 (eSewa): Expired → new purchase → fresh allowance (no rollover)")
    void testExpiredThenPurchase_esewa() {
        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub1 = getActiveSub();
        Long oldSubId = sub1.getId();
        createVehicle("T7-V1"); createVehicle("T7-V2"); // 2/3 used, 1 unused

        sub1.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.saveAndFlush(sub1);
        commitAndStart();

        completeEsewaPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertNotEquals(oldSubId, sub2.getId(), "New subscription row after expiry");
        assertEquals(3, sub2.getVehicleLimitSnapshot(), "Fresh allowance = 3 (no rollover from expired)");
        assertEquals(0, sub2.getCarriedForwardVehicleLimit(), "No carry-forward from expired sub");
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
    }

    // ==================================================================
    // TEST 8: Idempotent callback — no double rollover
    // ==================================================================
    @Test
    @DisplayName("TEST 8 (eSewa): Duplicate callback → no double rollover")
    void testIdempotentCallback_esewa() {
        TestableEsewaService esewa = createEsewaService();
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(planLimit1.getId());
        req.setBillingCycle("quarterly");
        var createResp = esewa.createPayment(req, owner.getId());
        Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                .orElseThrow();
        String total = pending.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        esewa.setMockStatus("COMPLETE", total, esewaConfig.getProductCode());

        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-IDEM", "REF-IDEM", "SUCCESS");
        Subscription sub1 = getActiveSub();
        int limitAfterFirst = sub1.getVehicleLimitSnapshot();
        LocalDateTime endDateAfterFirst = sub1.getEndDate();

        esewa.handleSuccessCallback(createResp.getTransactionUuid(), total,
                "TXN-IDEM", "REF-IDEM", "SUCCESS");
        Subscription sub2 = getActiveSub();

        assertEquals(limitAfterFirst, sub2.getVehicleLimitSnapshot(),
                "Duplicate callback must NOT change allowance");
        assertEquals(endDateAfterFirst, sub2.getEndDate(),
                "Duplicate callback must NOT extend expiry");
    }

    // ==================================================================
    // FONEPAY PARITY TESTS
    // ==================================================================

    @Test
    @DisplayName("TEST 1 (Fonepay): 0/3 → renewal → total allowance = 6")
    void testRenewalZeroUsed_fonepay() {
        completeFonepayPayment(planLimit1, "quarterly");
        LocalDateTime oldExpiry = getActiveSub().getEndDate();

        commitAndStart();
        completeFonepayPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(6, sub2.getVehicleLimitSnapshot(), "Fonepay: total = 6");
        assertEquals(3, sub2.getCarriedForwardVehicleLimit());
        assertEquals(oldExpiry.plusMonths(3).toLocalDate(), sub2.getEndDate().toLocalDate());
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
    }

    @Test
    @DisplayName("TEST 2 (Fonepay): 3/3 → renewal → total allowance = 3")
    void testRenewalAllUsed_fonepay() {
        completeFonepayPayment(planLimit1, "quarterly");
        backdatePeriodStart(10);
        commitAndStart();

        createVehicle("F2-V1"); createVehicle("F2-V2"); createVehicle("F2-V3");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()));
        commitAndStart();

        completeFonepayPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(3, sub2.getVehicleLimitSnapshot(), "Fonepay: total = 3");
        assertEquals(0, sub2.getCarriedForwardVehicleLimit());
        assertTrue(subscriptionAccessService.canAddVehicle(owner.getId()));
    }

    @Test
    @DisplayName("TEST 3 (Fonepay): 1/3 → renewal → total allowance = 5")
    void testRenewalPartialUsed_fonepay() {
        completeFonepayPayment(planLimit1, "quarterly");
        backdatePeriodStart(10);
        commitAndStart();

        createVehicle("F3-V1");
        commitAndStart();

        completeFonepayPayment(planLimit1, "quarterly");
        Subscription sub2 = getActiveSub();

        assertEquals(5, sub2.getVehicleLimitSnapshot(), "Fonepay: total = 5");
        assertEquals(2, sub2.getCarriedForwardVehicleLimit());
    }

    @Test
    @DisplayName("TEST 4 (Fonepay): Selling does NOT free a slot")
    void testSellingDoesNotFreeSlot_fonepay() {
        completeFonepayPayment(planLimit1, "quarterly");
        createVehicle("F4-V1"); createVehicle("F4-V2"); createVehicle("F4-V3");
        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()));

        Vehicle toSell = vehicleRepository.findByShopIdAndStatus(shop.getId(), VehicleStatus.ACTIVE).get(0);
        toSell.setStatus(VehicleStatus.SOLD);
        vehicleRepository.saveAndFlush(toSell);

        assertFalse(subscriptionAccessService.canAddVehicle(owner.getId()),
                "Fonepay: selling must NOT free a slot");
    }
}
