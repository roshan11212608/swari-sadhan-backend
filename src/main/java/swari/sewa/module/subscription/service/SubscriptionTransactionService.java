package swari.sewa.module.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.subscription.dto.*;

import java.time.LocalDateTime;

public interface SubscriptionTransactionService {
    Page<TransactionResponse> getTransactions(String search, String status, String gateway, String paymentMethod,
                                               Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate,
                                               Pageable pageable);
    String exportTransactionsCsv(String search, String status, String gateway, String paymentMethod,
                                  Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Aggregate figures over the entire filtered result set, so summary cards do
     * not have to be (incorrectly) derived from a single page of rows.
     */
    TransactionSummaryResponse getSummary(String search, String status, String gateway, String paymentMethod,
                                          Long shopOwnerId, Long planId,
                                          LocalDateTime fromDate, LocalDateTime toDate);

    /** COMPLETED transactions that carry an invoice number, filtered server-side. */
    Page<TransactionResponse> getInvoicedTransactions(String search, String gateway,
                                                     LocalDateTime fromDate, LocalDateTime toDate,
                                                     Pageable pageable);

    InvoiceSummaryResponse getInvoicedSummary(String search, String gateway,
                                              LocalDateTime fromDate, LocalDateTime toDate);
}
