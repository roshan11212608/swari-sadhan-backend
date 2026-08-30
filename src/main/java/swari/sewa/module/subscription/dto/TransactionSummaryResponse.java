package swari.sewa.module.subscription.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Aggregate figures for a transaction query, computed over the ENTIRE filtered
 * result set rather than the current page.
 *
 * <p>Page-scoped totals are misleading: a "Total Revenue" card that only sums
 * the ten rows currently visible is not total revenue. The frontend must render
 * these server-side values instead of reducing over {@code content}.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionSummaryResponse {

    /** Transactions matching the filters, all statuses. */
    private Long totalTransactions;

    private Long completedTransactions;
    private Long pendingTransactions;
    private Long failedTransactions;

    /** Sum of finalAmount across COMPLETED transactions only. */
    private BigDecimal totalRevenue;

    /** Sum of coupon discounts across COMPLETED transactions. */
    private BigDecimal totalDiscount;

    /** Sum of VAT across COMPLETED transactions. */
    private BigDecimal totalTax;

    /** Business currency the amounts are denominated in. */
    private String currency;

    public static TransactionSummaryResponse empty(String currency) {
        return TransactionSummaryResponse.builder()
                .totalTransactions(0L)
                .completedTransactions(0L)
                .pendingTransactions(0L)
                .failedTransactions(0L)
                .totalRevenue(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .currency(currency)
                .build();
    }
}
