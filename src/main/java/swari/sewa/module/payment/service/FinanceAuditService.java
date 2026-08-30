package swari.sewa.module.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Audit trail for finance-sensitive reads and exports.
 *
 * <p>Complements {@code SubscriptionAuditService}, which records administrative
 * *mutations* (plans created, coupons deleted, ...). Finance operations need the
 * *reads* recorded too: who looked at which invoice, and who exported the
 * transaction ledger. Those are the questions asked during a data-access review
 * or a customer dispute.
 *
 * <p>Emitted on a dedicated {@code FINANCE-AUDIT} logger so the events can be
 * routed to a separate appender/sink without pulling in unrelated application
 * logs. Correlation fields are included on every line so a support engineer can
 * trace Payment -> verification -> activation -> transaction -> invoice.
 *
 * <p>Never logs JWTs, invoice access tokens, gateway secrets or credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceAuditService {

    private static final org.slf4j.Logger AUDIT =
            org.slf4j.LoggerFactory.getLogger("FINANCE-AUDIT");

    /**
     * An invoice was rendered.
     *
     * @param transactionUuid  correlation id shared with Payment and SubscriptionTransaction
     * @param invoiceNumber    invoice identifier
     * @param ownerShopOwnerId the shop the invoice belongs to
     * @param actorRole        SUPERADMIN or SHOP_OWNER
     * @param actorId          the acting shop owner id, null for super admins
     */
    public void recordInvoiceViewed(String transactionUuid, String invoiceNumber,
                                    Long ownerShopOwnerId, String actorRole, Long actorId) {
        AUDIT.info("action=INVOICE_VIEWED transactionId={} invoiceNumber={} ownerShopOwnerId={} actorRole={} actorId={} crossShop={}",
                transactionUuid, invoiceNumber, ownerShopOwnerId, actorRole, actorId,
                actorId != null && !actorId.equals(ownerShopOwnerId));
    }

    /** The transaction ledger was exported to CSV. */
    public void recordTransactionsExported(String actorRole, Long actorId, int rowCount, String filterSummary) {
        AUDIT.info("action=TRANSACTIONS_EXPORTED actorRole={} actorId={} rows={} filters={}",
                actorRole, actorId, rowCount, filterSummary);
    }

    /** A payment reached a terminal state. */
    public void recordPaymentSettled(String transactionUuid, String gatewayTransactionId,
                                     String invoiceNumber, Long shopOwnerId, String status) {
        AUDIT.info("action=PAYMENT_SETTLED transactionId={} gatewayTransactionId={} invoiceNumber={} shopOwnerId={} status={}",
                transactionUuid, gatewayTransactionId, invoiceNumber, shopOwnerId, status);
    }

    /** A subscription was activated as a result of a payment. */
    public void recordSubscriptionActivated(String transactionUuid, Long subscriptionId, Long shopOwnerId) {
        AUDIT.info("action=SUBSCRIPTION_ACTIVATED transactionId={} subscriptionId={} shopOwnerId={}",
                transactionUuid, subscriptionId, shopOwnerId);
    }
}
