package swari.sewa.module.payment.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.EsewaSignatureService;
import swari.sewa.module.payment.service.FonepaySignatureService;
import swari.sewa.module.payment.service.PaymentEmailService;
import swari.sewa.module.payment.service.PaymentExpenseSyncService;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.payment.service.impl.FonepayPaymentServiceImpl;
import swari.sewa.module.subscription.entity.*;
import swari.sewa.module.subscription.enums.PlanCategory;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.*;
import swari.sewa.module.subscription.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Snapshot regression tests for eSewa and Fonepay payment gateways.
 *
 * Phase 2 discovered that Fonepay's activateSubscription() was NOT storing
 * plan snapshot fields (planNameSnapshot, vehicleLimitSnapshot, etc.) while
 * eSewa's implementation did. This was a production bug that was fixed.
 *
 * These regression tests ensure both gateways continue to store snapshots
 * correctly and that the snapshots remain equivalent.
 *
 * Tests verify:
 * - eSewa activation stores all snapshot fields
 * - Fonepay activation stores all snapshot fields
 * - Both gateways produce equivalent subscription state
 */
@SpringBootTest
@ActiveProfiles("integration")
class SnapshotRegressionTest {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private SubscriptionTransactionRepository transactionRepository;
    @Autowired private SubscriptionSettingsService settingsService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private PaymentEmailService paymentEmailService;
    @Autowired private PaymentExpenseSyncService paymentExpenseSyncService;
    @Autowired private SubscriptionPlanService planService;
    @Autowired private SubscriptionCouponService couponService;
    @Autowired private SubscriptionCouponUsageRepository couponUsageRepository;
    @Autowired private EsewaConfig esewaConfig;
    @Autowired private FonepayConfig fonepayConfig;

    private EsewaPaymentServiceImpl esewaService;
    private FonepayPaymentServiceImpl fonepayService;
    private SubscriptionPlan testPlan;

    @BeforeEach
    @Transactional
    void setUp() {
        EsewaSignatureService esewaSig = mock(EsewaSignatureService.class);
        FonepaySignatureService fonepaySig = mock(FonepaySignatureService.class);

        esewaService = new EsewaPaymentServiceImpl(
                paymentRepository, esewaConfig, esewaSig, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService,
                couponService, couponUsageRepository, mock(swari.sewa.module.payment.service.CouponUsageRecorder.class), mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));

        fonepayService = new FonepayPaymentServiceImpl(
                paymentRepository, fonepayConfig, fonepaySig, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService, mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));

        // Create a test plan with pricing and restrictions
        testPlan = SubscriptionPlan.builder()
                .name("Regression Test Plan")
                .slug("regression-test-" + System.nanoTime())
                .category(PlanCategory.STANDARD)
                .status(PlanStatus.PUBLISHED)
                .icon("test-icon")
                .themeColor("#3b82f6")
                .shortDescription("Test plan for regression")
                .description("Full description for testing")
                .build();

        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(testPlan)
                .monthly(new BigDecimal("599"))
                .quarterly(new BigDecimal("1499"))
                .halfYearly(new BigDecimal("2699"))
                .yearly(new BigDecimal("5399"))
                .currency("NPR")
                .build();
        Set<SubscriptionPlanPricing> pricings = new HashSet<>();
        pricings.add(pricing);
        testPlan.setPricings(pricings);

        SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                .plan(testPlan)
                .maxVehicles(10)
                .build();
        Set<SubscriptionPlanRestriction> restrictions = new HashSet<>();
        restrictions.add(restriction);
        testPlan.setRestrictions(restrictions);

        testPlan = planRepository.saveAndFlush(testPlan);
    }

    private Payment createPendingPayment(String txnUuid, String gateway) {
        return Payment.builder()
                .transactionUuid(txnUuid)
                .gateway(gateway)
                .shopOwnerId(800001L)
                .subscriptionPlanId(testPlan.getId())
                .billingCycle("yearly")
                .amount(new BigDecimal("5399.00"))
                .taxAmount(new BigDecimal("701.87"))
                .totalAmount(new BigDecimal("6100.87"))
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .productCode("EPAYTEST")
                .build();
    }

    // ===== eSewa Snapshot Regression =====

    @Nested
    @DisplayName("eSewa snapshot regression — all snapshot fields populated")
    class EsewaSnapshotRegressionTests {

        @Test
        @DisplayName("eSewa activation stores all plan snapshot fields")
        @Transactional
        void testEsewaSnapshot_allFieldsPopulated() {
            Payment payment = createPendingPayment("SS-ESEREG-001", "ESEWA");
            payment = paymentRepository.saveAndFlush(payment);

            // Use test subclass to mock eSewa verification
            TestableEsewaService testable = new TestableEsewaService(
                    paymentRepository, esewaConfig, mock(EsewaSignatureService.class), planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService,
                    couponService, couponUsageRepository, mock(swari.sewa.module.payment.service.CouponUsageRecorder.class), mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));
            testable.setStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", "EPAYTEST"));

            testable.handleSuccessCallback("SS-ESEREG-001", "6101", "TXN123", "REF456", "SUCCESS");

            // Verify subscription has all snapshot fields
            Optional<Subscription> subOpt = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getShopOwnerId().equals(800001L))
                    .findFirst();
            assertTrue(subOpt.isPresent(), "Subscription should be created");

            Subscription sub = subOpt.get();
            assertEquals("Regression Test Plan", sub.getPlanNameSnapshot(),
                    "planNameSnapshot must be populated");
            assertEquals("test-icon", sub.getPlanIconSnapshot(),
                    "planIconSnapshot must be populated");
            assertEquals("#3b82f6", sub.getPlanThemeColorSnapshot(),
                    "planThemeColorSnapshot must be populated");
            assertEquals(120, sub.getVehicleLimitSnapshot(),
                    "vehicleLimitSnapshot must be 10 × 12 = 120");
            assertEquals("yearly", sub.getBillingCycleSnapshot(),
                    "billingCycleSnapshot must be populated");
            assertEquals(new BigDecimal("5399.00"), sub.getPricePaid(),
                    "pricePaid must be populated");
            assertNotNull(sub.getPlanDescriptionSnapshot(),
                    "planDescriptionSnapshot must be populated");
            assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus(),
                    "Subscription must be ACTIVE");
        }
    }

    // ===== Fonepay Snapshot Regression =====

    @Nested
    @DisplayName("Fonepay snapshot regression — all snapshot fields populated (Phase 2 bug fix)")
    class FonepaySnapshotRegressionTests {

        @Test
        @DisplayName("Fonepay activation stores all plan snapshot fields")
        @Transactional
        void testFonepaySnapshot_allFieldsPopulated() {
            Payment payment = createPendingPayment("SS-FONEREG-001", "FONEPAY");
            payment = paymentRepository.saveAndFlush(payment);

            FonepaySignatureService fonepaySig = mock(FonepaySignatureService.class);
            when(fonepaySig.verifySignature(any(), any(), any())).thenReturn(true);

            FonepayPaymentServiceImpl fonepay = new FonepayPaymentServiceImpl(
                    paymentRepository, fonepayConfig, fonepaySig, planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService, mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));

            // Simulate Fonepay callback with valid signature, correct amount, success status
            // PID must match fonepay.merchant-code-pid (NBQM in default config)
            fonepay.handleCallback("SS-FONEREG-001", "NBQM", "success",
                    "6100.87", "UID123", "BID456", "valid-sig");

            // Verify subscription has all snapshot fields
            Optional<Subscription> subOpt = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getShopOwnerId().equals(800001L))
                    .findFirst();
            assertTrue(subOpt.isPresent(), "Subscription should be created");

            Subscription sub = subOpt.get();
            assertEquals("Regression Test Plan", sub.getPlanNameSnapshot(),
                    "Fonepay planNameSnapshot must be populated (Phase 2 bug fix)");
            assertEquals("test-icon", sub.getPlanIconSnapshot(),
                    "Fonepay planIconSnapshot must be populated");
            assertEquals("#3b82f6", sub.getPlanThemeColorSnapshot(),
                    "Fonepay planThemeColorSnapshot must be populated");
            assertEquals(120, sub.getVehicleLimitSnapshot(),
                    "Fonepay vehicleLimitSnapshot must be 10 × 12 = 120");
            assertEquals("yearly", sub.getBillingCycleSnapshot(),
                    "Fonepay billingCycleSnapshot must be populated");
            assertEquals(new BigDecimal("5399.00"), sub.getPricePaid(),
                    "Fonepay pricePaid must be populated");
            assertNotNull(sub.getPlanDescriptionSnapshot(),
                    "Fonepay planDescriptionSnapshot must be populated");
            assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus(),
                    "Fonepay subscription must be ACTIVE");
        }
    }

    // ===== Gateway Parity =====

    @Nested
    @DisplayName("Gateway parity — eSewa and Fonepay produce equivalent subscription state")
    class GatewayParityTests {

        @Test
        @DisplayName("eSewa and Fonepay produce equivalent subscription snapshots")
        @Transactional
        void testGatewayParity_equivalentSnapshots() {
            // eSewa payment
            Payment esewaPayment = createPendingPayment("SS-PARITY-E-001", "ESEWA");
            esewaPayment.setShopOwnerId(800002L);
            esewaPayment = paymentRepository.saveAndFlush(esewaPayment);

            TestableEsewaService testable = new TestableEsewaService(
                    paymentRepository, esewaConfig, mock(EsewaSignatureService.class), planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService,
                    couponService, couponUsageRepository, mock(swari.sewa.module.payment.service.CouponUsageRecorder.class), mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));
            testable.setStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", "EPAYTEST"));
            testable.handleSuccessCallback("SS-PARITY-E-001", "6101", "TXN123", "REF456", "SUCCESS");

            // Fonepay payment
            Payment fonepayPayment = createPendingPayment("SS-PARITY-F-001", "FONEPAY");
            fonepayPayment.setShopOwnerId(800003L);
            fonepayPayment = paymentRepository.saveAndFlush(fonepayPayment);

            FonepaySignatureService fonepaySig = mock(FonepaySignatureService.class);
            when(fonepaySig.verifySignature(any(), any(), any())).thenReturn(true);
            FonepayPaymentServiceImpl fonepay = new FonepayPaymentServiceImpl(
                    paymentRepository, fonepayConfig, fonepaySig, planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService, mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));
            fonepay.handleCallback("SS-PARITY-F-001", "NBQM", "success",
                    "6100.87", "UID123", "BID456", "valid-sig");

            // Compare subscriptions
            Subscription esewaSub = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getShopOwnerId().equals(800002L)).findFirst().orElseThrow();
            Subscription fonepaySub = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getShopOwnerId().equals(800003L)).findFirst().orElseThrow();

            // Business state must be equivalent
            assertEquals(esewaSub.getStatus(), fonepaySub.getStatus(),
                    "Both must be ACTIVE");
            assertEquals(esewaSub.getPlanNameSnapshot(), fonepaySub.getPlanNameSnapshot(),
                    "Plan name snapshot must match");
            assertEquals(esewaSub.getVehicleLimitSnapshot(), fonepaySub.getVehicleLimitSnapshot(),
                    "Vehicle limit snapshot must match");
            assertEquals(esewaSub.getBillingCycleSnapshot(), fonepaySub.getBillingCycleSnapshot(),
                    "Billing cycle snapshot must match");
            assertEquals(esewaSub.getPricePaid(), fonepaySub.getPricePaid(),
                    "Price paid must match");
            assertEquals(esewaSub.getPlanIconSnapshot(), fonepaySub.getPlanIconSnapshot(),
                    "Icon snapshot must match");
            assertEquals(esewaSub.getPlanThemeColorSnapshot(), fonepaySub.getPlanThemeColorSnapshot(),
                    "Theme color snapshot must match");
        }
    }

    // ===== Test Subclass =====

    private static class TestableEsewaService extends EsewaPaymentServiceImpl {
        private EsewaStatusResult mockResult;

        TestableEsewaService(
                PaymentRepository paymentRepository, swari.sewa.module.payment.config.EsewaConfig esewaConfig,
                EsewaSignatureService signatureService, SubscriptionPlanService planService,
                SubscriptionRepository subscriptionRepository,
                SubscriptionTransactionRepository transactionRepository,
                InvoiceService invoiceService, PaymentEmailService paymentEmailService,
                PaymentExpenseSyncService paymentExpenseSyncService,
                SubscriptionSettingsService settingsService,
                SubscriptionCouponService couponService,
                SubscriptionCouponUsageRepository couponUsageRepository,
                swari.sewa.module.payment.service.CouponUsageRecorder couponUsageRecorder,
                swari.sewa.module.vehicle.repository.VehicleRepository vehicleRepository) {
            super(paymentRepository, esewaConfig, signatureService, planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService,
                    couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);
        }

        void setStatusResult(EsewaStatusResult result) {
            this.mockResult = result;
        }

        @Override
        protected EsewaStatusResult verifyWithEsewa(Payment payment) {
            if (mockResult != null) return mockResult;
            throw new IllegalStateException("Test must set mock eSewa status result");
        }
    }
}


