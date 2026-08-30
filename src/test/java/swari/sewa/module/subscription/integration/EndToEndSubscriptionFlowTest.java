package swari.sewa.module.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;
import jakarta.persistence.EntityManager;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.common.util.JwtUtil;
import swari.sewa.module.category.entity.Category;
import swari.sewa.module.category.repository.CategoryRepository;
import swari.sewa.module.expense.entity.Expense;
import swari.sewa.module.expense.repository.ExpenseRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive end-to-end subscription lifecycle integration test.
 *
 * Uses the real application context, real MySQL database (swarisadhan_test),
 * real services, real repositories, and real Flyway-managed schema.
 *
 * The ONLY mocked boundary is the external eSewa gateway status API
 * (via TestableEsewaService subclass that overrides verifyWithEsewa).
 *
 * Workflow tested:
 *   Shop Owner → Trial → Trial vehicle limit → Yearly plan → Coupon → VAT →
 *   Payment created → Payment SUCCESS → Subscription ACTIVE → Snapshot verified →
 *   Vehicle limit (10×12=120) → Grandfathered vehicles → Add to limit →
 *   Sell doesn't free slot → Billing history → Invoice → Expense sync
 *
 * Tenant isolation:
 *   Owner A vs Owner B — cross-access denied for subscription, payment, invoice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class EndToEndSubscriptionFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private ShopOwnerRepository shopOwnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private SubscriptionTransactionRepository transactionRepository;
    @Autowired private SubscriptionCouponRepository couponRepository;
    @Autowired private SubscriptionCouponUsageRepository couponUsageRepository;
    @Autowired private SubscriptionSettingsRepository settingsRepository;
    @Autowired private SubscriptionTrialConfigRepository trialConfigRepository;
    @Autowired private ExpenseRepository expenseRepository;

    @Autowired private EsewaConfig esewaConfig;
    @Autowired private EsewaSignatureService esewaSignatureService;
    @Autowired private SubscriptionPlanService planService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private PaymentEmailService paymentEmailService;
    @Autowired private PaymentExpenseSyncService paymentExpenseSyncService;
    @Autowired private SubscriptionSettingsService settingsService;
    @Autowired private SubscriptionCouponService couponService;
    @Autowired private CouponUsageRecorder couponUsageRecorder;
    @Autowired private SubscriptionAccessService subscriptionAccessService;
    @Autowired private TrialSubscriptionService trialSubscriptionService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager entityManager;

    private SubscriptionPlan testPlan;
    private ShopOwner ownerA;
    private ShopOwner ownerB;
    private Shop shopA;
    private Shop shopB;
    private String ownerAToken;
    private String ownerBToken;
    private SubscriptionTrialConfig trialConfig;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Ensure singleton settings exist
        SubscriptionSettings settings = settingsRepository.findById(1L).orElseGet(() ->
                SubscriptionSettings.builder()
                        .id(1L)
                        .enableVat(true)
                        .taxPercentage(13)
                        .currency("NPR")
                        .invoicePrefix("INV")
                        .build());
        settings.setEnableVat(true);
        settings.setTaxPercentage(13);
        settingsRepository.saveAndFlush(settings);

        // Create or update trial config (singleton with ID=1)
        trialConfig = trialConfigRepository.findById(1L).orElseGet(() ->
                SubscriptionTrialConfig.builder()
                        .id(1L)
                        .name("Default Trial")
                        .active(true)
                        .duration(14)
                        .vehicleLimit(3)
                        .build());
        trialConfig.setActive(true);
        trialConfig.setDuration(14);
        trialConfig.setVehicleLimit(3);
        trialConfigRepository.saveAndFlush(trialConfig);

        // Create a published plan with monthly limit 10, yearly price 5399
        testPlan = SubscriptionPlan.builder()
                .name("E2E Pro Plan " + System.nanoTime())
                .slug("e2e-pro-" + System.nanoTime())
                .category(PlanCategory.PREMIUM)
                .status(PlanStatus.PUBLISHED)
                .description("E2E test plan")
                .shortDescription("E2E Pro")
                .icon("test-icon")
                .themeColor("#3b82f6")
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
                .maxVehicles(10) // monthly limit
                .build();
        Set<SubscriptionPlanRestriction> restrictions = new HashSet<>();
        restrictions.add(restriction);
        testPlan.setRestrictions(restrictions);

        testPlan = planRepository.saveAndFlush(testPlan);
        trialConfig.setTrialPlanId(testPlan.getId());
        trialConfigRepository.saveAndFlush(trialConfig);

        // Use unique suffix per test method to avoid duplicate key conflicts
        String suffix = String.valueOf(System.nanoTime());

        // Create a category for vehicles (category_id is non-null FK)
        testCategory = Category.builder()
                .name("E2E Category " + suffix)
                .build();
        testCategory = categoryRepository.saveAndFlush(testCategory);

        // Create Owner A
        ownerA = createShopOwner("e2e-a-" + suffix + "@swari.com", "9800000101");
        ownerAToken = jwtUtil.generateToken(ownerA.getEmail(), ownerA.getRole().name());

        // Create Owner B
        ownerB = createShopOwner("e2e-b-" + suffix + "@swari.com", "9800000102");
        ownerBToken = jwtUtil.generateToken(ownerB.getEmail(), ownerB.getRole().name());

        // Create shops for both owners (required for expense sync and vehicle FK)
        shopA = createShop(ownerA, "E2E Shop A " + suffix, "E2E-LIC-A-" + suffix);
        shopB = createShop(ownerB, "E2E Shop B " + suffix, "E2E-LIC-B-" + suffix);
    }

    private ShopOwner createShopOwner(String email, String phone) {
        // Create the User record first (shops.user_id is non-null FK)
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("TestPass123!"))
                .firstName("E2E")
                .lastName("Owner")
                .phoneNumber(phone)
                .role(UserRole.SHOP_OWNER)
                .isActive(true)
                .isEmailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);

        ShopOwner owner = ShopOwner.builder()
                .firstName("E2E")
                .lastName("Owner")
                .email(email)
                .password(passwordEncoder.encode("TestPass123!"))
                .phone(phone)
                .role(UserRole.SHOP_OWNER)
                .active(true)
                .emailVerified(true)
                .approvalStatus("APPROVED")
                .passwordChanged(true)
                .build();
        owner = shopOwnerRepository.saveAndFlush(owner);
        return owner;
    }

    private Shop createShop(ShopOwner owner, String name, String license) {
        User user = userRepository.findByEmail(owner.getEmail()).orElseThrow();
        Shop shop = Shop.builder()
                .name(name)
                .description("E2E test shop")
                .licenseNumber(license)
                .city("Kathmandu")
                .state("Bagmati")
                .country("Nepal")
                .status(ShopStatus.ACTIVE)
                .isFeatured(false)
                .shopOwner(owner)
                .user(user)
                .build();
        return shopRepository.saveAndFlush(shop);
    }

    private Vehicle createVehicle(Shop shop, String title, LocalDateTime createdAt) {
        Vehicle v = Vehicle.builder()
                .title(title)
                .vehicleType(VehicleType.BIKE)
                .brandName("Test")
                .modelName("Model")
                .manufacturingYear(2023)
                .fuelType("Petrol")
                .transmissionType("Manual")
                .price(new BigDecimal("50000.00"))
                .status(VehicleStatus.ACTIVE)
                .shop(shop)
                .category(testCategory)
                .build();
        v = vehicleRepository.saveAndFlush(v);
        // Manually set createdAt for grandfathering tests
        if (createdAt != null) {
            entityManager.createNativeQuery("UPDATE vehicles SET created_at = :createdAt WHERE id = :id")
                    .setParameter("createdAt", createdAt)
                    .setParameter("id", v.getId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }
        return v;
    }

    /**
     * Testable eSewa service that mocks only the external gateway status API.
     * Everything else uses real services, repositories, and database.
     */
    private TestableEsewaService createTestableEsewaService() {
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

    // ======================================================================
    // PART 1: FULL E2E SUBSCRIPTION WORKFLOW
    // ======================================================================

    @Nested
    @DisplayName("Full E2E subscription workflow: trial → payment → active → vehicles → billing → invoice → expense")
    class FullSubscriptionWorkflow {

        @Test
        @DisplayName("Complete subscription lifecycle from trial to active with vehicle limits")
        void testCompleteSubscriptionLifecycle() {
            // === STEP 1: Trial creation ===
            // Trial service uses REQUIRES_NEW, so commit setup data first
            TestTransaction.flagForCommit();
            TestTransaction.end();

            boolean trialStarted = trialSubscriptionService.startTrialIfNeeded(ownerA.getId());
            assertTrue(trialStarted, "Trial should be started for new shop owner");

            // Start a new transaction for the rest of the test
            TestTransaction.start();

            // Verify trial subscription exists
            List<Subscription> trialSubs = subscriptionRepository
                    .findByShopOwnerIdAndStatus(ownerA.getId(), SubscriptionStatus.TRIAL);
            assertEquals(1, trialSubs.size(), "Exactly one TRIAL subscription should exist");
            Subscription trialSub = trialSubs.get(0);
            assertEquals(SubscriptionStatus.TRIAL, trialSub.getStatus());
            assertNotNull(trialSub.getEndDate(), "Trial must have end date");
            assertEquals(3, trialSub.getVehicleLimitSnapshot(),
                    "Trial vehicle limit should come from trial config (3)");

            // === STEP 2: Trial vehicle access ===
            assertTrue(subscriptionAccessService.hasVehicleAccess(ownerA.getId()),
                    "Trial should grant vehicle access");
            assertTrue(subscriptionAccessService.canAddVehicle(ownerA.getId()),
                    "Should be able to add vehicles during trial");

            // === STEP 3: Create 5 vehicles BEFORE paid subscription (grandfathered) ===
            LocalDateTime beforeSub = LocalDateTime.now().minusMinutes(5);
            for (int i = 0; i < 5; i++) {
                createVehicle(shopA, "Pre-sub vehicle " + i, beforeSub);
            }

            // === STEP 4: Create a coupon ===
            SubscriptionCoupon coupon = SubscriptionCoupon.builder()
                    .code("E2ECOUPON" + System.nanoTime())
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .percentage(10)
                    .usageLimit(100)
                    .active(true)
                    .build();
            coupon = couponRepository.saveAndFlush(coupon);

            // === STEP 5: Create eSewa payment with coupon (yearly plan) ===
            CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
            paymentRequest.setPlanId(testPlan.getId());
            paymentRequest.setBillingCycle("yearly");
            paymentRequest.setCouponCode(coupon.getCode());

            TestableEsewaService esewa = createTestableEsewaService();
            var createResponse = esewa.createPayment(paymentRequest, ownerA.getId());

            assertNotNull(createResponse.getTransactionUuid());
            assertNotNull(createResponse.getPaymentUrl());

            // Verify payment record in DB
            Payment pendingPayment = paymentRepository.findByTransactionUuid(createResponse.getTransactionUuid())
                    .orElseThrow();
            assertEquals(PaymentStatus.PENDING, pendingPayment.getStatus());
            assertEquals(new BigDecimal("5399.00"), pendingPayment.getAmount(),
                    "Plan price from DB (yearly = 5399)");
            assertNotNull(pendingPayment.getCouponId(), "Coupon should be attached");
            assertEquals(coupon.getId(), pendingPayment.getCouponId());

            // Verify discount: 10% of 5399 = 539.90
            BigDecimal expectedDiscount = new BigDecimal("539.90");
            assertEquals(expectedDiscount, pendingPayment.getDiscountAmount(),
                    "Coupon discount should be 10% of 5399");

            // Verify VAT: 13% of (5399 - 539.90) = 13% of 4859.10 = 631.68
            BigDecimal amountAfterDiscount = new BigDecimal("5399.00").subtract(expectedDiscount);
            BigDecimal expectedVat = amountAfterDiscount.multiply(new BigDecimal("13"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            assertEquals(expectedVat, pendingPayment.getTaxAmount(),
                    "VAT should be 13% of amount after coupon discount");

            // Verify total = amountAfterDiscount + VAT
            BigDecimal expectedTotal = amountAfterDiscount.add(expectedVat)
                    .setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedTotal, pendingPayment.getTotalAmount(),
                    "Total = (price - discount) + VAT");

            // === STEP 6: Simulate eSewa gateway callback (mocked at external boundary) ===
            // eSewa returns amounts as integers
            String esewaTotalAmount = pendingPayment.getTotalAmount()
                    .setScale(0, RoundingMode.HALF_UP).toPlainString();
            esewa.setMockStatus("COMPLETE", esewaTotalAmount, esewaConfig.getProductCode());

            var successResponse = esewa.handleSuccessCallback(
                    createResponse.getTransactionUuid(),
                    esewaTotalAmount,
                    "ESEWA-TXN-001",
                    "ESEWA-REF-001",
                    "SUCCESS");

            // === STEP 7: Verify subscription is ACTIVE ===
            Payment successPayment = paymentRepository.findByTransactionUuid(createResponse.getTransactionUuid())
                    .orElseThrow();
            assertEquals(PaymentStatus.SUCCESS, successPayment.getStatus(),
                    "Payment should be SUCCESS after verification");
            assertNotNull(successPayment.getSubscriptionId(),
                    "Subscription ID should be set after activation");
            assertNotNull(successPayment.getInvoiceNumber(),
                    "Invoice number should be generated");

            Subscription activeSub = subscriptionRepository.findById(successPayment.getSubscriptionId())
                    .orElseThrow();
            assertEquals(SubscriptionStatus.ACTIVE, activeSub.getStatus(),
                    "Subscription should be ACTIVE");

            // === STEP 8: Verify all snapshot fields ===
            assertEquals(testPlan.getName(), activeSub.getPlanNameSnapshot(),
                    "planNameSnapshot must match plan name at purchase time");
            assertEquals("test-icon", activeSub.getPlanIconSnapshot(),
                    "planIconSnapshot must be populated");
            assertEquals("#3b82f6", activeSub.getPlanThemeColorSnapshot(),
                    "planThemeColorSnapshot must be populated");
            assertNotNull(activeSub.getPlanDescriptionSnapshot(),
                    "planDescriptionSnapshot must be populated");
            assertEquals(new BigDecimal("5399.00"), activeSub.getPricePaid(),
                    "pricePaid must be the plan price (before discount)");
            assertEquals("yearly", activeSub.getBillingCycleSnapshot(),
                    "billingCycleSnapshot must be yearly");

            // === STEP 9: Verify vehicle limit = 10 × 12 = 120 ===
            assertEquals(120, activeSub.getVehicleLimitSnapshot(),
                    "Vehicle limit should be monthly(10) × monthsInCycle(12) = 120");

            // === STEP 10: Verify transaction record created ===
            List<SubscriptionTransaction> txns = transactionRepository.findAll().stream()
                    .filter(t -> ownerA.getId().equals(t.getShopOwnerId()))
                    .toList();
            assertFalse(txns.isEmpty(), "Transaction record should be created");
            SubscriptionTransaction txn = txns.stream()
                    .filter(t -> t.getTransactionId().equals(createResponse.getTransactionUuid()))
                    .findFirst().orElseThrow();
            assertEquals(TransactionStatus.COMPLETED, txn.getStatus());
            assertNotNull(txn.getInvoiceNumber(), "Transaction should have invoice number");

            // === STEP 11: Verify expense synchronization ===
            Optional<Expense> expenseOpt = expenseRepository.findByReferenceNumber(
                    createResponse.getTransactionUuid());
            assertTrue(expenseOpt.isPresent(), "Expense should be auto-created for successful payment");
            Expense expense = expenseOpt.get();
            assertEquals(successPayment.getTotalAmount(), expense.getAmount(),
                    "Expense amount should match payment total");
            assertEquals("PAID", expense.getPaymentStatus().name(),
                    "Expense should be marked as PAID");

            // === STEP 12: Verify billing history via controller ===
            try {
                mockMvc.perform(get("/api/payments/billing-history")
                                .header("Authorization", "Bearer " + ownerAToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[?(@.transactionUuid == '" +
                                createResponse.getTransactionUuid() + "')]").exists());
            } catch (Exception e) {
                fail("Billing history request failed: " + e.getMessage());
            }

            // === STEP 13: Verify invoice download accessible ===
            try {
                mockMvc.perform(get("/api/payments/invoice/" + createResponse.getTransactionUuid())
                                .header("Authorization", "Bearer " + ownerAToken))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                fail("Invoice download should succeed for owner's own payment: " + e.getMessage());
            }

            // === STEP 14: Coupon usage is recorded via afterCommit synchronization.
            // Commit the current transaction so the afterCommit callback fires.
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            // === STEP 15: Verify coupon usage recorded ===
            long usageCount = couponUsageRepository.countByCouponId(coupon.getId());
            assertEquals(1, usageCount, "Coupon usage should be recorded once");

            // === STEP 16: Idempotency — duplicate callback should not create duplicates ===
            var duplicateResponse = esewa.handleSuccessCallback(
                    createResponse.getTransactionUuid(),
                    esewaTotalAmount,
                    "ESEWA-TXN-001",
                    "ESEWA-REF-001",
                    "SUCCESS");

            // Verify no duplicate subscriptions, transactions, or expenses
            long subCount = subscriptionRepository
                    .findByShopOwnerIdAndStatus(ownerA.getId(), SubscriptionStatus.ACTIVE)
                    .size();
            assertEquals(1, subCount, "Duplicate callback must not create another subscription");

            long expenseCount = expenseRepository.findByReferenceNumber(
                    createResponse.getTransactionUuid()).stream().count();
            assertEquals(1, expenseCount, "Duplicate callback must not create another expense");
        }
    }

    // ======================================================================
    // PART 2: VEHICLE LIMIT END-TO-END
    // ======================================================================

    @Nested
    @DisplayName("Vehicle limit: grandfathered vehicles, add to limit, sell doesn't free slot")
    class VehicleLimitEndToEnd {

        @Test
        @DisplayName("Grandfathered vehicles don't count; add to limit; sell doesn't free slot")
        void testVehicleLimitWithGrandfatheringAndSelling() {
            // Create 5 vehicles BEFORE subscription
            LocalDateTime beforeSub = LocalDateTime.now().minusDays(1);
            for (int i = 0; i < 5; i++) {
                createVehicle(shopA, "Grandfathered " + i, beforeSub);
            }

            // Create an ACTIVE subscription with a small limit for test speed
            // Use monthly limit 3, yearly → 3 × 12 = 36 total
            // But for faster testing, use monthly limit 2, monthly cycle → 2 × 1 = 2
            SubscriptionPlan smallPlan = SubscriptionPlan.builder()
                    .name("Small Plan " + System.nanoTime())
                    .slug("small-" + System.nanoTime())
                    .category(PlanCategory.BASIC)
                    .status(PlanStatus.PUBLISHED)
                    .icon("icon")
                    .themeColor("#000")
                    .build();
            SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                    .plan(smallPlan)
                    .monthly(new BigDecimal("99"))
                    .quarterly(new BigDecimal("299"))
                    .halfYearly(new BigDecimal("599"))
                    .yearly(new BigDecimal("1199"))
                    .currency("NPR")
                    .build();
            smallPlan.setPricings(Set.of(pricing));
            SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                    .plan(smallPlan)
                    .maxVehicles(2) // monthly limit = 2
                    .build();
            smallPlan.setRestrictions(Set.of(restriction));
            smallPlan = planRepository.saveAndFlush(smallPlan);

            // Create payment and activate
            CreatePaymentRequest req = new CreatePaymentRequest();
            req.setPlanId(smallPlan.getId());
            req.setBillingCycle("monthly"); // 2 × 1 = 2 vehicle limit

            TestableEsewaService esewa = createTestableEsewaService();
            var createResp = esewa.createPayment(req, ownerA.getId());

            Payment pending = paymentRepository.findByTransactionUuid(createResp.getTransactionUuid())
                    .orElseThrow();
            String esewaTotal = pending.getTotalAmount()
                    .setScale(0, RoundingMode.HALF_UP).toPlainString();
            esewa.setMockStatus("COMPLETE", esewaTotal, esewaConfig.getProductCode());
            esewa.handleSuccessCallback(createResp.getTransactionUuid(), esewaTotal,
                    "TXN-VL-001", "REF-VL-001", "SUCCESS");

            // Verify subscription is active with limit 2
            Subscription sub = subscriptionRepository
                    .findByShopOwnerIdAndStatus(ownerA.getId(), SubscriptionStatus.ACTIVE)
                    .get(0);
            assertEquals(2, sub.getVehicleLimitSnapshot(),
                    "Vehicle limit should be 2 (monthly 2 × 1 month)");

            // Verify grandfathered vehicles don't count
            long usageCount = vehicleRepository
                    .countByShop_ShopOwner_IdAndCreatedAtAfter(ownerA.getId(), sub.getCurrentPeriodStart());
            assertEquals(0, usageCount, "No vehicles added after subscription yet");

            assertTrue(subscriptionAccessService.canAddVehicle(ownerA.getId()),
                    "Should be able to add (0 < 2)");

            // Add vehicle 1 (count = 1, limit = 2) → allowed
            createVehicle(shopA, "Post-sub 1", null);
            assertTrue(subscriptionAccessService.canAddVehicle(ownerA.getId()),
                    "Should be able to add (1 < 2)");

            // Add vehicle 2 (count = 2, limit = 2) → allowed (at limit)
            createVehicle(shopA, "Post-sub 2", null);
            assertFalse(subscriptionAccessService.canAddVehicle(ownerA.getId()),
                    "Should NOT be able to add (2 >= 2)");

            // Attempt to add vehicle 3 → should throw
            assertThrows(SubscriptionLimitExceededException.class,
                    () -> subscriptionAccessService.validateCanAddVehicle(ownerA.getId()),
                    "Adding beyond limit should throw SubscriptionLimitExceededException");

            // Sell one vehicle (mark as SOLD)
            List<Vehicle> vehicles = vehicleRepository.findByShopIdAndStatus(shopA.getId(), VehicleStatus.ACTIVE);
            Vehicle toSell = vehicles.stream()
                    .findFirst().orElseThrow();
            toSell.setStatus(VehicleStatus.SOLD);
            vehicleRepository.saveAndFlush(toSell);

            // Selling does NOT free an add slot — count still includes SOLD vehicles
            // The count is based on createdAt >= subscriptionStart, regardless of status
            assertFalse(subscriptionAccessService.canAddVehicle(ownerA.getId()),
                    "Selling should NOT free an add slot — limit still reached");

            // Attempting to add after selling should still throw
            assertThrows(SubscriptionLimitExceededException.class,
                    () -> subscriptionAccessService.validateCanAddVehicle(ownerA.getId()),
                    "Adding after sell should still throw — sell doesn't free slot");
        }
    }

    // ======================================================================
    // PART 3: TENANT ISOLATION
    // ======================================================================

    @Nested
    @DisplayName("Tenant isolation: Owner A cannot access Owner B's data")
    class TenantIsolation {

        @Test
        @DisplayName("Cross-owner invoice access denied; billing history scoped to owner")
        void testCrossOwnerIsolation() throws Exception {
            // Create payment + subscription for Owner A
            CreatePaymentRequest reqA = new CreatePaymentRequest();
            reqA.setPlanId(testPlan.getId());
            reqA.setBillingCycle("yearly");

            TestableEsewaService esewaA = createTestableEsewaService();
            var respA = esewaA.createPayment(reqA, ownerA.getId());
            Payment pendingA = paymentRepository.findByTransactionUuid(respA.getTransactionUuid())
                    .orElseThrow();
            String totalA = pendingA.getTotalAmount()
                    .setScale(0, RoundingMode.HALF_UP).toPlainString();
            esewaA.setMockStatus("COMPLETE", totalA, esewaConfig.getProductCode());
            esewaA.handleSuccessCallback(respA.getTransactionUuid(), totalA,
                    "TXN-A-001", "REF-A-001", "SUCCESS");

            // Create payment + subscription for Owner B
            CreatePaymentRequest reqB = new CreatePaymentRequest();
            reqB.setPlanId(testPlan.getId());
            reqB.setBillingCycle("monthly");

            TestableEsewaService esewaB = createTestableEsewaService();
            var respB = esewaB.createPayment(reqB, ownerB.getId());
            Payment pendingB = paymentRepository.findByTransactionUuid(respB.getTransactionUuid())
                    .orElseThrow();
            String totalB = pendingB.getTotalAmount()
                    .setScale(0, RoundingMode.HALF_UP).toPlainString();
            esewaB.setMockStatus("COMPLETE", totalB, esewaConfig.getProductCode());
            esewaB.handleSuccessCallback(respB.getTransactionUuid(), totalB,
                    "TXN-B-001", "REF-B-001", "SUCCESS");

            // Owner A billing history should contain only A's payment
            mockMvc.perform(get("/api/payments/billing-history")
                            .header("Authorization", "Bearer " + ownerAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.transactionUuid == '" +
                            respA.getTransactionUuid() + "')]").exists())
                    .andExpect(jsonPath("$.data[?(@.transactionUuid == '" +
                            respB.getTransactionUuid() + "')]").doesNotExist());

            // Owner B billing history should contain only B's payment
            mockMvc.perform(get("/api/payments/billing-history")
                            .header("Authorization", "Bearer " + ownerBToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.transactionUuid == '" +
                            respB.getTransactionUuid() + "')]").exists())
                    .andExpect(jsonPath("$.data[?(@.transactionUuid == '" +
                            respA.getTransactionUuid() + "')]").doesNotExist());

            // Owner A can download own invoice
            mockMvc.perform(get("/api/payments/invoice/" + respA.getTransactionUuid())
                            .header("Authorization", "Bearer " + ownerAToken))
                    .andExpect(status().isOk());

            // Owner A CANNOT download Owner B's invoice → 403
            mockMvc.perform(get("/api/payments/invoice/" + respB.getTransactionUuid())
                            .header("Authorization", "Bearer " + ownerAToken))
                    .andExpect(status().isForbidden());

            // Owner B CANNOT download Owner A's invoice → 403
            mockMvc.perform(get("/api/payments/invoice/" + respA.getTransactionUuid())
                            .header("Authorization", "Bearer " + ownerBToken))
                    .andExpect(status().isForbidden());

            // Unknown transaction UUID → 404
            mockMvc.perform(get("/api/payments/invoice/NON-EXISTENT-UUID")
                            .header("Authorization", "Bearer " + ownerAToken))
                    .andExpect(status().isNotFound());

            // Subscriptions are isolated
            List<Subscription> subsA = subscriptionRepository
                    .findByShopOwnerIdAndStatus(ownerA.getId(), SubscriptionStatus.ACTIVE);
            List<Subscription> subsB = subscriptionRepository
                    .findByShopOwnerIdAndStatus(ownerB.getId(), SubscriptionStatus.ACTIVE);
            assertEquals(1, subsA.size(), "Owner A should have 1 active subscription");
            assertEquals(1, subsB.size(), "Owner B should have 1 active subscription");
            assertNotEquals(subsA.get(0).getId(), subsB.get(0).getId(),
                    "Owners must have different subscriptions");

            // Expenses are isolated per owner's shop
            Optional<Expense> expenseA = expenseRepository.findByReferenceNumber(respA.getTransactionUuid());
            Optional<Expense> expenseB = expenseRepository.findByReferenceNumber(respB.getTransactionUuid());
            assertTrue(expenseA.isPresent(), "Expense for A should exist");
            assertTrue(expenseB.isPresent(), "Expense for B should exist");
            assertEquals(shopA.getId(), expenseA.get().getShop().getId(),
                    "Expense A should belong to shop A");
            assertEquals(shopB.getId(), expenseB.get().getShop().getId(),
                    "Expense B should belong to shop B");
        }
    }
}
