package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.InvoiceSummaryResponse;
import swari.sewa.module.subscription.dto.TransactionResponse;
import swari.sewa.module.subscription.dto.TransactionSummaryResponse;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.TransactionStatus;
import swari.sewa.module.subscription.repository.SubscriptionCouponRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;
import swari.sewa.module.subscription.service.SubscriptionTransactionService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTransactionServiceImpl implements SubscriptionTransactionService {

    private final SubscriptionTransactionRepository transactionRepository;
    private final SubscriptionCouponRepository couponRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final SubscriptionSettingsService settingsService;

    @Value("${app.finance.export.max-rows:50000}")
    private int exportMaxRows;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(String search, String status, String gateway, String paymentMethod,
                                                      Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate,
                                                      Pageable pageable) {
        log.info("Fetching transactions with filters - search: {}, status: {}, gateway: {}, paymentMethod: {}, shopOwnerId: {}, planId: {}, fromDate: {}, toDate: {}",
                search, status, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);

        TransactionStatus statusEnum = parseStatus(status);
        Page<SubscriptionTransaction> page = transactionRepository.findWithFilters(
                search, statusEnum, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate, pageable);

        // Resolve shop names for the whole page in one query instead of one per
        // row, which previously produced an N+1 pattern.
        Map<Long, String> shopNames = resolveShopNames(page.getContent());
        String currency = currentCurrency();
        return page.map(txn -> mapToResponse(txn, shopNames, currency));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryResponse getSummary(String search, String status, String gateway, String paymentMethod,
                                                 Long shopOwnerId, Long planId,
                                                 LocalDateTime fromDate, LocalDateTime toDate) {
        TransactionStatus statusEnum = parseStatus(status);
        String currency = currentCurrency();

        List<Object[]> rows = transactionRepository.getFilteredSummary(
                search, statusEnum, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);
        if (rows.isEmpty() || rows.get(0) == null) {
            return TransactionSummaryResponse.empty(currency);
        }

        Object[] r = rows.get(0);
        return TransactionSummaryResponse.builder()
                .totalTransactions(asLong(r[0]))
                .completedTransactions(asLong(r[1]))
                .pendingTransactions(asLong(r[2]))
                .failedTransactions(asLong(r[3]))
                .totalRevenue(asDecimal(r[4]))
                .totalDiscount(asDecimal(r[5]))
                .totalTax(asDecimal(r[6]))
                .currency(currency)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getInvoicedTransactions(String search, String gateway,
                                                            LocalDateTime fromDate, LocalDateTime toDate,
                                                            Pageable pageable) {
        Page<SubscriptionTransaction> page = transactionRepository.findInvoiced(
                search, gateway, fromDate, toDate, pageable);
        Map<Long, String> shopNames = resolveShopNames(page.getContent());
        String currency = currentCurrency();
        return page.map(txn -> mapToResponse(txn, shopNames, currency));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceSummaryResponse getInvoicedSummary(String search, String gateway,
                                                     LocalDateTime fromDate, LocalDateTime toDate) {
        String currency = currentCurrency();
        List<Object[]> rows = transactionRepository.getInvoicedSummary(search, gateway, fromDate, toDate);
        if (rows.isEmpty() || rows.get(0) == null) {
            return InvoiceSummaryResponse.empty(currency);
        }
        Object[] r = rows.get(0);
        return InvoiceSummaryResponse.builder()
                .totalInvoices(asLong(r[0]))
                .totalBilled(asDecimal(r[1]))
                .totalTax(asDecimal(r[2]))
                .totalDiscount(asDecimal(r[3]))
                .currency(currency)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportTransactionsCsv(String search, String status, String gateway, String paymentMethod,
                                         Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Exporting transactions to CSV with filters");

        TransactionStatus statusEnum = parseStatus(status);
        List<SubscriptionTransaction> transactions = transactionRepository.findForExport(
                search, statusEnum, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);

        boolean truncated = transactions.size() > exportMaxRows;
        if (truncated) {
            log.warn("CSV export matched {} rows, exceeding the {} row cap — truncating",
                    transactions.size(), exportMaxRows);
            transactions = transactions.subList(0, exportMaxRows);
        }

        Map<Long, String> shopNames = resolveShopNames(transactions);

        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Invoice Number,Shop Owner ID,Shop Name,Plan,Coupon,Subtotal,VAT,Discount,Total,Currency,Payment Method,Gateway,Status,Date\n");

        String currency = currentCurrency();
        for (SubscriptionTransaction txn : transactions) {
            csv.append(csvCell(txn.getTransactionId())).append(",")
                    .append(csvCell(txn.getInvoiceNumber())).append(",")
                    .append(txn.getShopOwnerId() != null ? txn.getShopOwnerId() : "").append(",")
                    .append(csvCell(shopNames.get(txn.getShopOwnerId()))).append(",")
                    .append(csvCell(txn.getPlan() != null ? txn.getPlan().getName() : "")).append(",")
                    .append(csvCell(couponCodeOf(txn))).append(",")
                    .append(plain(txn.getAmount())).append(",")
                    .append(plain(txn.getTax())).append(",")
                    .append(plain(txn.getDiscount())).append(",")
                    .append(plain(txn.getFinalAmount())).append(",")
                    .append(csvCell(currency)).append(",")
                    .append(csvCell(txn.getPaymentMethod())).append(",")
                    .append(csvCell(txn.getGateway())).append(",")
                    .append(txn.getStatus() != null ? txn.getStatus().name() : "").append(",")
                    .append(txn.getTransactionDate() != null ? txn.getTransactionDate().toString() : "")
                    .append("\n");
        }

        if (truncated) {
            csv.append("# Export truncated at ").append(exportMaxRows)
                    .append(" rows. Narrow the filters to export the remainder.\n");
        }

        log.info("CSV export completed with {} transactions", transactions.size());
        return csv.toString();
    }

    // ===== Mapping =====

    private TransactionResponse mapToResponse(SubscriptionTransaction txn,
                                              Map<Long, String> shopNames,
                                              String currency) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionId(txn.getTransactionId())
                .shopOwnerId(txn.getShopOwnerId())
                .shopName(shopNames.get(txn.getShopOwnerId()))
                .subscriptionId(txn.getSubscriptionId())
                .planId(txn.getPlan() != null ? txn.getPlan().getId() : null)
                .planName(txn.getPlan() != null ? txn.getPlan().getName() : null)
                .amount(txn.getAmount())
                .tax(txn.getTax())
                .couponCode(couponCodeOf(txn))
                .discount(txn.getDiscount())
                .finalAmount(txn.getFinalAmount())
                .paymentMethod(txn.getPaymentMethod())
                .gateway(txn.getGateway())
                .status(txn.getStatus() != null ? txn.getStatus().name() : null)
                .invoiceNumber(txn.getInvoiceNumber())
                .transactionDate(txn.getTransactionDate())
                .currency(currency)
                .build();
    }

    /**
     * Coupon code for display. The snapshot taken at payment time wins, because
     * coupons are hard-deleted and can be renamed — resolving the live coupon by
     * id would silently rewrite history. The live lookup remains only as a
     * fallback for rows created before the snapshot column existed.
     */
    private String couponCodeOf(SubscriptionTransaction txn) {
        if (txn.getCouponCodeSnapshot() != null && !txn.getCouponCodeSnapshot().isBlank()) {
            return txn.getCouponCodeSnapshot();
        }
        if (txn.getCouponId() == null) {
            return null;
        }
        return couponRepository.findById(txn.getCouponId())
                .map(swari.sewa.module.subscription.entity.SubscriptionCoupon::getCode)
                .orElse(null);
    }

    /** Batch-load shop display names for a page of transactions (avoids N+1). */
    private Map<Long, String> resolveShopNames(List<SubscriptionTransaction> transactions) {
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = transactions.stream()
                .map(SubscriptionTransaction::getShopOwnerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (ShopOwner owner : shopOwnerRepository.findAllById(ids)) {
            names.put(owner.getId(), displayName(owner));
        }
        return names;
    }

    private String displayName(ShopOwner owner) {
        if (owner.getShopName() != null && !owner.getShopName().isBlank()) {
            return owner.getShopName();
        }
        String first = owner.getFirstName() != null ? owner.getFirstName() : "";
        String last = owner.getLastName() != null ? owner.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    private String currentCurrency() {
        var settings = settingsService.getSettingsEntity();
        return settings != null && settings.getCurrency() != null ? settings.getCurrency() : "NPR";
    }

    /**
     * Parse a client-supplied status. Unknown values are rejected with a clear
     * message rather than surfacing an IllegalArgumentException from valueOf.
     */
    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported transaction status: " + status);
        }
    }

    private static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        return value instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }

    private static String plain(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    /**
     * Render a value as a CSV cell.
     *
     * <p>Two separate concerns are handled here:
     * <ul>
     *   <li><b>CSV quoting</b> — commas, quotes, CR and LF are escaped so the
     *       column layout cannot be broken.</li>
     *   <li><b>Formula injection</b> — Excel, LibreOffice and Google Sheets treat
     *       a leading {@code = + - @} (and tab/CR) as the start of a formula, so a
     *       stored value like {@code =HYPERLINK(...)} would execute on open. Such
     *       values are prefixed with a single quote, which spreadsheets strip on
     *       display but do not evaluate.</li>
     * </ul>
     */
    private String csvCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = neutralizeFormula(value);
        if (sanitized.indexOf(',') >= 0 || sanitized.indexOf('"') >= 0
                || sanitized.indexOf('\n') >= 0 || sanitized.indexOf('\r') >= 0) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    private String neutralizeFormula(String value) {
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
