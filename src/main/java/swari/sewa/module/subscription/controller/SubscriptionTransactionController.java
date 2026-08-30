package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.service.SubscriptionTransactionService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/superadmin/subscription/transactions")
@RequiredArgsConstructor
public class SubscriptionTransactionController {

    private final SubscriptionTransactionService transactionService;

    /**
     * Sortable fields, explicitly allowlisted.
     *
     * <p>The sort field reaches Hibernate as an entity property name and ends up
     * in the generated ORDER BY. Accepting arbitrary client input there allows
     * probing the entity model and can surface internal columns or provoke
     * errors, so anything not on this list is rejected.
     *
     * <p>Keys are the names clients may send; values are the entity properties.
     */
    private static final Map<String, String> SORTABLE = Map.of(
            "transactionDate", "transactionDate",
            "finalAmount", "finalAmount",
            "amount", "amount",
            "status", "status",
            "createdAt", "createdAt"
    );

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long shopOwnerId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "transactionDate,desc") String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<TransactionResponse> transactions = transactionService.getTransactions(
                search, status, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions, "Transactions retrieved successfully"));
    }

    /**
     * Aggregates over the whole filtered set. The transactions page renders these
     * instead of summing the rows it happens to be displaying.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> getSummary(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long shopOwnerId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        TransactionSummaryResponse summary = transactionService.getSummary(
                search, status, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Transaction summary retrieved successfully"));
    }

    /**
     * Invoiced transactions: COMPLETED and carrying an invoice number. Filtered
     * in the database so the client never over-fetches to filter locally.
     */
    @GetMapping("/invoiced")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getInvoiced(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "transactionDate,desc") String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<TransactionResponse> invoices = transactionService.getInvoicedTransactions(
                search, gateway, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices retrieved successfully"));
    }

    @GetMapping("/invoiced/summary")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<InvoiceSummaryResponse>> getInvoicedSummary(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        InvoiceSummaryResponse summary = transactionService.getInvoicedSummary(search, gateway, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Invoice summary retrieved successfully"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<String> exportTransactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long shopOwnerId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        String csvContent = transactionService.exportTransactionsCsv(
                search, status, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=transactions.csv")
                // Never let a browser or shared cache retain financial exports.
                .header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(csvContent);
    }

    /**
     * Build a Pageable with a validated sort field and a bounded page size.
     */
    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String[] parts = sort == null ? new String[0] : sort.split(",");
        String requested = parts.length > 0 ? parts[0].trim() : "transactionDate";
        String property = SORTABLE.get(requested);
        if (property == null) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: " + requested + ". Allowed: " + Set.copyOf(SORTABLE.keySet()));
        }
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
