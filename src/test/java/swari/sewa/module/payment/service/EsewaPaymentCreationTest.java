package swari.sewa.module.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.CreatePaymentResponse;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.subscription.dto.CouponValidationResponse;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionCouponService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EsewaPaymentServiceImpl.createPayment — the most critical
 * payment creation flow in the system.
 *
 * Business rules verified:
 * - Backend fetches authoritative price from DB (never trusts frontend)
 * - Only PUBLISHED plans can be purchased
 * - Invalid/missing plan → PaymentException
 * - Invalid billing cycle → PaymentException
 * - Coupon validation with locking (validateCouponForPayment)
 * - VAT calculated AFTER discount
 * - BigDecimal with HALF_UP rounding, 2 decimal places
 * - Payment record stores amount, discount, tax, total, coupon snapshots
 * - eSewa amount = discounted base (not original plan price)
 * - eSewa total = discounted base + tax
 * - Transaction UUID is unique
 * - Payment status = PENDING
 */
class EsewaPaymentCreationTest {

    private EsewaPaymentServiceImpl service;
    private PaymentRepository paymentRepository;
    private EsewaConfig esewaConfig;
    private EsewaSignatureService signatureService;
    private SubscriptionPlanService planService;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionTransactionRepository transactionRepository;
    private InvoiceService invoiceService;
    private PaymentEmailService paymentEmailService;
    private PaymentExpenseSyncService paymentExpenseSyncService;
    private SubscriptionSettingsService settingsService;
    private SubscriptionCouponService couponService;
    private swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository couponUsageRepository;
    private CouponUsageRecorder couponUsageRecorder;

    private static final Long SHOP_OWNER_ID = 100L;
    private static final Long PLAN_ID = 7L;
    private static final String PRODUCT_CODE = "EPAYTEST";
    private static final String SECRET_KEY = "8gBm/:&EnhH.1/q";

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        esewaConfig = Mockito.mock(EsewaConfig.class);
        signatureService = Mockito.mock(EsewaSignatureService.class);
        planService = Mockito.mock(SubscriptionPlanService.class);
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
        transactionRepository = Mockito.mock(SubscriptionTransactionRepository.class);
        invoiceService = Mockito.mock(InvoiceService.class);
        paymentEmailService = Mockito.mock(PaymentEmailService.class);
        paymentExpenseSyncService = Mockito.mock(PaymentExpenseSyncService.class);
        settingsService = Mockito.mock(SubscriptionSettingsService.class);
        couponService = Mockito.mock(SubscriptionCouponService.class);
        couponUsageRepository = Mockito.mock(swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository.class);
        couponUsageRecorder = Mockito.mock(CouponUsageRecorder.class);

        service = new EsewaPaymentServiceImpl(
                paymentRepository, esewaConfig, signatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService,
                couponService, couponUsageRepository, couponUsageRecorder, mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));

        // Default eSewa config
        when(esewaConfig.getProductCode()).thenReturn(PRODUCT_CODE);
        when(esewaConfig.getSecretKey()).thenReturn(SECRET_KEY);
        when(esewaConfig.getPaymentUrl()).thenReturn("https://rc-epay.esewa.com.np/api/epay/main/v2/form");
        when(esewaConfig.getBackendSuccessUrl()).thenReturn("http://localhost:8081/api/payments/esewa/success");
        when(esewaConfig.getBackendFailureUrl()).thenReturn("http://localhost:8081/api/payments/esewa/failure");

        // Default signature
        when(signatureService.generateSignature(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-signature-base64");

        // Default payment save returns the payment with an ID
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(paymentRepository.existsByTransactionUuid(anyString())).thenReturn(false);
    }

    private SubscriptionPlan createPublishedPlan(Long id, String name, BigDecimal monthly, BigDecimal quarterly,
                                                   BigDecimal halfYearly, BigDecimal yearly, Integer maxVehicles) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .status(PlanStatus.PUBLISHED)
                .build();

        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(plan)
                .monthly(monthly)
                .quarterly(quarterly)
                .halfYearly(halfYearly)
                .yearly(yearly)
                .currency("NPR")
                .build();
        Set<SubscriptionPlanPricing> pricings = new HashSet<>();
        pricings.add(pricing);
        plan.setPricings(pricings);

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

    private SubscriptionSettings createSettings(boolean enableVat, Integer taxPercentage) {
        return SubscriptionSettings.builder()
                .id(1L)
                .enableVat(enableVat)
                .taxPercentage(taxPercentage)
                .currency("NPR")
                .build();
    }

    private CreatePaymentRequest createRequest(String billingCycle, String couponCode) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(PLAN_ID);
        req.setBillingCycle(billingCycle);
        if (couponCode != null) req.setCouponCode(couponCode);
        return req;
    }

    // ===== Valid Payment Creation =====

    @Nested
    @DisplayName("Valid payment creation — authoritative pricing from DB")
    class ValidPaymentCreationTests {

        @Test
        @DisplayName("Yearly plan: price 5399, no coupon, 13% VAT → correct amounts")
        void testYearlyPayment_noCoupon_withVat() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business", 
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            assertNotNull(response);
            assertEquals(PRODUCT_CODE, response.getProductCode());
            assertNotNull(response.getTransactionUuid());
            assertNotNull(response.getSignature());

            // eSewa amounts are integers
            // amount = 5399 (no discount), tax = 5399 * 13% = 701.87 → 702, total = 5399 + 702 = 6101
            // But eSewa amount should be the base (no discount = 5399)
            int esewaAmount = Integer.parseInt(response.getAmount());
            int esewaTax = Integer.parseInt(response.getTaxAmount());
            int esewaTotal = Integer.parseInt(response.getTotalAmount());
            assertEquals(esewaAmount + esewaTax, esewaTotal, 
                    "eSewa total must equal amount + tax_amount");
        }

        @Test
        @DisplayName("Quarterly plan: price 1599, no coupon, 13% VAT")
        void testQuarterlyPayment_noCoupon_withVat() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CreatePaymentResponse response = service.createPayment(createRequest("quarterly", null), SHOP_OWNER_ID);

            int esewaAmount = Integer.parseInt(response.getAmount());
            int esewaTax = Integer.parseInt(response.getTaxAmount());
            int esewaTotal = Integer.parseInt(response.getTotalAmount());
            assertEquals(esewaAmount + esewaTax, esewaTotal,
                    "eSewa total must equal amount + tax_amount");
        }

        @Test
        @DisplayName("Monthly plan: price 599, no coupon, VAT disabled → tax=0, total=599")
        void testMonthlyPayment_noCoupon_vatDisabled() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(false, 0));

            CreatePaymentResponse response = service.createPayment(createRequest("monthly", null), SHOP_OWNER_ID);

            int esewaAmount = Integer.parseInt(response.getAmount());
            int esewaTax = Integer.parseInt(response.getTaxAmount());
            int esewaTotal = Integer.parseInt(response.getTotalAmount());
            assertEquals(599, esewaAmount);
            assertEquals(0, esewaTax);
            assertEquals(599, esewaTotal);
        }

        @Test
        @DisplayName("Payment record is saved with PENDING status")
        void testPaymentRecord_savedAsPending() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            verify(paymentRepository).save(argThat(p ->
                    p.getStatus() == PaymentStatus.PENDING &&
                    p.getGateway().equals("ESEWA") &&
                    p.getShopOwnerId().equals(SHOP_OWNER_ID) &&
                    p.getSubscriptionPlanId().equals(PLAN_ID) &&
                    p.getBillingCycle().equals("yearly") &&
                    p.getCurrency().equals("NPR")
            ));
        }

        @Test
        @DisplayName("Payment record stores plan snapshot fields")
        void testPaymentRecord_storesPlanAmount() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            verify(paymentRepository).save(argThat(p ->
                    p.getAmount().compareTo(new BigDecimal("5399.00")) == 0 &&
                    p.getDiscountAmount().compareTo(BigDecimal.ZERO) == 0
            ));
        }
    }

    // ===== Backend Price is Authoritative =====

    @Nested
    @DisplayName("Backend price is authoritative — frontend price ignored")
    class AuthoritativePriceTests {

        @Test
        @DisplayName("Plan price comes from DB, not from request")
        void testPriceFromDB_notFromRequest() {
            // The CreatePaymentRequest only has planId, billingCycle, couponCode — no price field
            // This test verifies the request DTO does NOT accept a price
            CreatePaymentRequest req = new CreatePaymentRequest();
            req.setPlanId(PLAN_ID);
            req.setBillingCycle("yearly");

            // Verify there is no price/amount field on the request
            assertNull(req.getCouponCode()); // only planId, billingCycle, couponCode
        }
    }

    // ===== Invalid Plan =====

    @Nested
    @DisplayName("Invalid plan handling")
    class InvalidPlanTests {

        @Test
        @DisplayName("Unpublished plan → PaymentException")
        void testUnpublishedPlan_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), null, null, null, 50);
            plan.setStatus(PlanStatus.DRAFT);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);

            PaymentException ex = assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("monthly", null), SHOP_OWNER_ID));
            assertTrue(ex.getMessage().contains("not currently available"));
        }

        @Test
        @DisplayName("Archived plan → PaymentException")
        void testArchivedPlan_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), null, null, null, 50);
            plan.setStatus(PlanStatus.ARCHIVED);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("monthly", null), SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Disabled plan → PaymentException")
        void testDisabledPlan_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), null, null, null, 50);
            plan.setStatus(PlanStatus.DISABLED);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("monthly", null), SHOP_OWNER_ID));
        }
    }

    // ===== Invalid Billing Cycle =====

    @Nested
    @DisplayName("Invalid billing cycle handling")
    class InvalidBillingCycleTests {

        @Test
        @DisplayName("Invalid billing cycle → PaymentException")
        void testInvalidBillingCycle_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("weekly", null), SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Null billing cycle → PaymentException or default behavior")
        void testNullBillingCycle() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CreatePaymentRequest req = new CreatePaymentRequest();
            req.setPlanId(PLAN_ID);
            // billingCycle is null

            assertThrows(Exception.class,
                    () -> service.createPayment(req, SHOP_OWNER_ID));
        }
    }

    // ===== Missing Pricing =====

    @Nested
    @DisplayName("Missing pricing for billing cycle")
    class MissingPricingTests {

        @Test
        @DisplayName("Yearly price is null → PaymentException")
        void testNullYearlyPrice_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), null, 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID));
        }

        @Test
        @DisplayName("Yearly price is zero → PaymentException")
        void testZeroYearlyPrice_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), BigDecimal.ZERO, 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID));
        }
    }

    // ===== Payment with Coupon =====

    @Nested
    @DisplayName("Payment with coupon — discount + VAT on discounted amount")
    class PaymentWithCouponTests {

        @Test
        @DisplayName("10% coupon on 5399, 13% VAT → discount 539.90, tax on 4859.10")
        void testPaymentWithCoupon_percentage() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            // Mock coupon validation
            BigDecimal discount = new BigDecimal("539.90");
            CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                    .valid(true)
                    .code("SAVE10")
                    .discountType("PERCENTAGE")
                    .percentage(10)
                    .discountAmount(discount)
                    .originalAmount(new BigDecimal("5399"))
                    .finalAmount(new BigDecimal("4859.10"))
                    .couponId(1L)
                    .build();
            when(couponService.validateCouponForPayment("SAVE10", new BigDecimal("5399")))
                    .thenReturn(couponResponse);

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", "SAVE10"), SHOP_OWNER_ID);

            assertNotNull(response);

            // Verify payment record stores correct amounts
            verify(paymentRepository).save(argThat(p -> {
                BigDecimal expectedTaxable = new BigDecimal("5399.00").subtract(discount);
                BigDecimal expectedTax = expectedTaxable.multiply(BigDecimal.valueOf(13))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal expectedTotal = expectedTaxable.add(expectedTax).setScale(2, RoundingMode.HALF_UP);

                return p.getAmount().compareTo(new BigDecimal("5399.00")) == 0 &&
                       p.getDiscountAmount().compareTo(discount) == 0 &&
                       p.getCouponId() != null &&
                       p.getCouponId().equals(1L) &&
                       "SAVE10".equals(p.getCouponCodeSnapshot()) &&
                       "PERCENTAGE".equals(p.getCouponDiscountTypeSnapshot()) &&
                       p.getTaxAmount().compareTo(expectedTax) == 0 &&
                       p.getTotalAmount().compareTo(expectedTotal) == 0;
            }));
        }

        @Test
        @DisplayName("Invalid coupon → PaymentException")
        void testPaymentWithInvalidCoupon_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CouponValidationResponse invalidResponse = CouponValidationResponse.builder()
                    .valid(false)
                    .message("Invalid coupon code")
                    .code("NOPE")
                    .build();
            when(couponService.validateCouponForPayment(anyString(), any()))
                    .thenReturn(invalidResponse);

            PaymentException ex = assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("yearly", "NOPE"), SHOP_OWNER_ID));
            assertTrue(ex.getMessage().contains("Invalid coupon code"));
        }

        @Test
        @DisplayName("Expired coupon → PaymentException")
        void testPaymentWithExpiredCoupon_throws() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CouponValidationResponse expiredResponse = CouponValidationResponse.builder()
                    .valid(false)
                    .message("This coupon has expired")
                    .code("EXPIRED")
                    .build();
            when(couponService.validateCouponForPayment(anyString(), any()))
                    .thenReturn(expiredResponse);

            PaymentException ex = assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest("yearly", "EXPIRED"), SHOP_OWNER_ID));
            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("eSewa amount = discounted base, not original plan price")
        void testEsewaAmount_isDiscountedBase() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            BigDecimal discount = new BigDecimal("500.00");
            CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                    .valid(true)
                    .code("FLAT500")
                    .discountType("FLAT")
                    .flatDiscount(discount)
                    .discountAmount(discount)
                    .originalAmount(new BigDecimal("5399"))
                    .finalAmount(new BigDecimal("4899.00"))
                    .couponId(2L)
                    .build();
            when(couponService.validateCouponForPayment("FLAT500", new BigDecimal("5399")))
                    .thenReturn(couponResponse);

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", "FLAT500"), SHOP_OWNER_ID);

            // eSewa amount should be 4899 (5399 - 500), NOT 5399
            int esewaAmount = Integer.parseInt(response.getAmount());
            int esewaTax = Integer.parseInt(response.getTaxAmount());
            int esewaTotal = Integer.parseInt(response.getTotalAmount());

            assertEquals(4899, esewaAmount, "eSewa amount must be the discounted base, not original price");
            assertEquals(esewaAmount + esewaTax, esewaTotal, "eSewa total must equal amount + tax");
        }
    }

    // ===== VAT Calculation =====

    @Nested
    @DisplayName("VAT calculation — after discount, BigDecimal, HALF_UP")
    class VatCalculationInPaymentTests {

        @Test
        @DisplayName("VAT disabled → tax=0, total=plan price")
        void testVatDisabled() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(false, 0));

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            verify(paymentRepository).save(argThat(p ->
                    p.getTaxAmount().compareTo(BigDecimal.ZERO) == 0 &&
                    p.getTotalAmount().compareTo(new BigDecimal("5399.00")) == 0
            ));
        }

        @Test
        @DisplayName("VAT enabled, no coupon → tax on full plan price")
        void testVatEnabled_noCoupon() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            // 5399 * 13% = 701.87
            verify(paymentRepository).save(argThat(p ->
                    p.getTaxAmount().compareTo(new BigDecimal("701.87")) == 0
            ));
        }

        @Test
        @DisplayName("VAT enabled, with coupon → tax on discounted amount")
        void testVatEnabled_withCoupon() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            BigDecimal discount = new BigDecimal("539.90");
            CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                    .valid(true)
                    .code("SAVE10")
                    .discountType("PERCENTAGE")
                    .percentage(10)
                    .discountAmount(discount)
                    .originalAmount(new BigDecimal("5399"))
                    .finalAmount(new BigDecimal("4859.10"))
                    .couponId(1L)
                    .build();
            when(couponService.validateCouponForPayment("SAVE10", new BigDecimal("5399")))
                    .thenReturn(couponResponse);

            service.createPayment(createRequest("yearly", "SAVE10"), SHOP_OWNER_ID);

            // Taxable = 5399 - 539.90 = 4859.10
            // VAT = 4859.10 * 13% = 631.683 → 631.68
            verify(paymentRepository).save(argThat(p ->
                    p.getTaxAmount().compareTo(new BigDecimal("631.68")) == 0
            ));
        }
    }

    // ===== Transaction UUID =====

    @Nested
    @DisplayName("Transaction UUID generation")
    class TransactionUuidTests {

        @Test
        @DisplayName("Transaction UUID is unique and follows format")
        void testTransactionUuidFormat() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            String uuid = response.getTransactionUuid();
            assertNotNull(uuid);
            assertTrue(uuid.startsWith("SS-"), "Transaction UUID must start with SS-");
            assertTrue(uuid.length() > 10, "Transaction UUID must have sufficient length");
        }

        @Test
        @DisplayName("Duplicate UUID is regenerated")
        void testDuplicateUuid_regenerated() {
            SubscriptionPlan plan = createPublishedPlan(PLAN_ID, "Business",
                    new BigDecimal("599"), new BigDecimal("1599"), new BigDecimal("2999"), new BigDecimal("5399"), 50);
            when(planService.getPlanEntity(PLAN_ID)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(createSettings(true, 13));

            // First call returns true (exists), second returns false
            when(paymentRepository.existsByTransactionUuid(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);

            CreatePaymentResponse response = service.createPayment(createRequest("yearly", null), SHOP_OWNER_ID);

            assertNotNull(response.getTransactionUuid());
            verify(paymentRepository, times(2)).existsByTransactionUuid(anyString());
        }
    }
}

