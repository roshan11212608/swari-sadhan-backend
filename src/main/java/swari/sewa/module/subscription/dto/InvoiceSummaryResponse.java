package swari.sewa.module.subscription.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Aggregate figures for the Super Admin Invoices view, computed over the entire
 * filtered invoiced set (COMPLETED transactions that carry an invoice number).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceSummaryResponse {

    private Long totalInvoices;

    /** Sum of finalAmount across the invoiced set. */
    private BigDecimal totalBilled;

    /** Sum of VAT collected. */
    private BigDecimal totalTax;

    /** Sum of coupon discounts granted. */
    private BigDecimal totalDiscount;

    private String currency;

    public static InvoiceSummaryResponse empty(String currency) {
        return InvoiceSummaryResponse.builder()
                .totalInvoices(0L)
                .totalBilled(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .currency(currency)
                .build();
    }
}
