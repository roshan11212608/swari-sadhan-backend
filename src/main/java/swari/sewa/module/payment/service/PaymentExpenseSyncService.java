package swari.sewa.module.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swari.sewa.common.enums.ExpensePaymentMethod;
import swari.sewa.common.enums.ExpensePaymentStatus;
import swari.sewa.module.expense.entity.Expense;
import swari.sewa.module.expense.entity.ExpenseCategory;
import swari.sewa.module.expense.repository.ExpenseCategoryRepository;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.subscription.entity.SubscriptionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Auto-creates an expense record when a subscription payment succeeds,
 * so the payment shows up in the shop owner's expenses list.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpenseSyncService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ShopRepository shopRepository;

    private static final String SUBSCRIPTION_CATEGORY = "Subscription Payment";

    /**
     * Create an expense entry for a successful subscription payment.
     * Idempotent: skips if an expense with the same reference number already exists.
     */
    public void createSubscriptionExpense(Payment payment, SubscriptionPlan plan) {
        try {
            // Idempotency: skip if already created for this payment
            if (expenseRepository.findByReferenceNumber(payment.getTransactionUuid()).isPresent()) {
                log.info("Expense already exists for payment {}, skipping", payment.getTransactionUuid());
                return;
            }

            // Find the shop for this shop owner
            List<Shop> shops = shopRepository.findByShopOwnerId(payment.getShopOwnerId());
            if (shops.isEmpty()) {
                log.warn("No shop found for shop_owner={}, skipping expense creation", payment.getShopOwnerId());
                return;
            }
            Shop shop = shops.get(0);

            // Find or create the "Subscription Payment" category
            ExpenseCategory category = expenseCategoryRepository.findByName(SUBSCRIPTION_CATEGORY)
                    .orElseGet(() -> {
                        ExpenseCategory cat = ExpenseCategory.builder()
                                .name(SUBSCRIPTION_CATEGORY)
                                .color("#f97316")
                                .icon("credit-card")
                                .isActive(true)
                                .build();
                        return expenseCategoryRepository.save(cat);
                    });

            String expenseNumber = expenseRepository.generateNextExpenseNumber();

            Expense expense = Expense.builder()
                    .expenseNumber(expenseNumber)
                    .shop(shop)
                    .category(category)
                    .title("Swari Sadhan " + plan.getName() + " Plan - " + payment.getBillingCycle())
                    .amount(payment.getTotalAmount() != null ? payment.getTotalAmount() : payment.getAmount())
                    .expenseDate(payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate() : LocalDate.now())
                    .description("Subscription payment for " + plan.getName() + " plan (" + payment.getBillingCycle() + " billing cycle). Invoice: " + payment.getInvoiceNumber())
                    .vendorPaidTo("MYCON Digital - Swari Sadhan")
                    .paymentMethod(mapGatewayToPaymentMethod(payment.getGateway()))
                    .paymentStatus(ExpensePaymentStatus.PAID)
                    .referenceNumber(payment.getTransactionUuid())
                    .isActive(true)
                    .createdBy("SYSTEM")
                    .build();

            expenseRepository.save(expense);
            log.info("Created expense {} for subscription payment {} (invoice {})",
                    expenseNumber, payment.getTransactionUuid(), payment.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Failed to create expense for payment {}: {}", payment.getTransactionUuid(), e.getMessage(), e);
            // Don't throw — expense creation failure should not break payment flow
        }
    }

    private ExpensePaymentMethod mapGatewayToPaymentMethod(String gateway) {
        if (gateway == null) return ExpensePaymentMethod.BANK_TRANSFER;
        switch (gateway.toUpperCase()) {
            case "ESEWA":
            case "FONEPAY":
                return ExpensePaymentMethod.UPI;
            case "CARD":
                return ExpensePaymentMethod.CARD;
            default:
                return ExpensePaymentMethod.BANK_TRANSFER;
        }
    }
}
