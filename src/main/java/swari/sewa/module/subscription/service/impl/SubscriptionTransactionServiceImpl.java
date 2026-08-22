package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.TransactionResponse;
import swari.sewa.module.subscription.entity.SubscriptionCoupon;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.TransactionStatus;
import swari.sewa.module.subscription.repository.SubscriptionCouponRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.SubscriptionTransactionService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTransactionServiceImpl implements SubscriptionTransactionService {

    private final SubscriptionTransactionRepository transactionRepository;
    private final SubscriptionCouponRepository couponRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(String search, String status, String gateway, String paymentMethod,
                                                      Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate,
                                                      Pageable pageable) {
        log.info("Fetching transactions with filters - search: {}, status: {}, gateway: {}, paymentMethod: {}, shopOwnerId: {}, planId: {}, fromDate: {}, toDate: {}",
                search, status, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);

        TransactionStatus statusEnum = (status != null && !status.isEmpty()) ? TransactionStatus.valueOf(status.toUpperCase()) : null;
        return transactionRepository.findWithFilters(search, statusEnum, gateway, paymentMethod,
                        shopOwnerId, planId, fromDate, toDate, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportTransactionsCsv(String search, String status, String gateway, String paymentMethod,
                                         Long shopOwnerId, Long planId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Exporting transactions to CSV with filters");

        TransactionStatus statusEnum = (status != null && !status.isEmpty()) ? TransactionStatus.valueOf(status.toUpperCase()) : null;
        List<SubscriptionTransaction> transactions = transactionRepository.findForExport(
                search, statusEnum, gateway, paymentMethod, shopOwnerId, planId, fromDate, toDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Invoice Number,Shop Owner ID,Plan,Amount,Tax,Discount,Final Amount,Payment Method,Gateway,Status,Date\n");

        for (SubscriptionTransaction txn : transactions) {
            String planName = txn.getPlan() != null ? txn.getPlan().getName() : "";
            String txnId = escapeCsv(txn.getTransactionId());
            String invoiceNumber = escapeCsv(txn.getInvoiceNumber());
            String amount = txn.getAmount() != null ? txn.getAmount().toPlainString() : "";
            String tax = txn.getTax() != null ? txn.getTax().toPlainString() : "";
            String discount = txn.getDiscount() != null ? txn.getDiscount().toPlainString() : "";
            String finalAmount = txn.getFinalAmount() != null ? txn.getFinalAmount().toPlainString() : "";
            String paymentMethodVal = escapeCsv(txn.getPaymentMethod());
            String gatewayVal = escapeCsv(txn.getGateway());
            String statusVal = txn.getStatus() != null ? txn.getStatus().name() : "";
            String date = txn.getTransactionDate() != null ? txn.getTransactionDate().toString() : "";

            csv.append(txnId).append(",")
                    .append(invoiceNumber).append(",")
                    .append(txn.getShopOwnerId()).append(",")
                    .append(escapeCsv(planName)).append(",")
                    .append(amount).append(",")
                    .append(tax).append(",")
                    .append(discount).append(",")
                    .append(finalAmount).append(",")
                    .append(paymentMethodVal).append(",")
                    .append(gatewayVal).append(",")
                    .append(statusVal).append(",")
                    .append(date).append("\n");
        }

        log.info("CSV export completed with {} transactions", transactions.size());
        return csv.toString();
    }

    private TransactionResponse mapToResponse(SubscriptionTransaction txn) {
        String shopName = resolveShopName(txn.getShopOwnerId());
        String couponCode = resolveCouponCode(txn.getCouponId());

        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionId(txn.getTransactionId())
                .shopOwnerId(txn.getShopOwnerId())
                .shopName(shopName)
                .subscriptionId(txn.getSubscriptionId())
                .planId(txn.getPlan() != null ? txn.getPlan().getId() : null)
                .planName(txn.getPlan() != null ? txn.getPlan().getName() : null)
                .amount(txn.getAmount())
                .tax(txn.getTax())
                .couponCode(couponCode)
                .discount(txn.getDiscount())
                .finalAmount(txn.getFinalAmount())
                .paymentMethod(txn.getPaymentMethod())
                .gateway(txn.getGateway())
                .status(txn.getStatus() != null ? txn.getStatus().name() : null)
                .invoiceNumber(txn.getInvoiceNumber())
                .transactionDate(txn.getTransactionDate())
                .build();
    }

    private String resolveShopName(Long shopOwnerId) {
        if (shopOwnerId == null) {
            return null;
        }
        Optional<ShopOwner> shopOwner = shopOwnerRepository.findById(shopOwnerId);
        return shopOwner.map(owner -> owner.getShopName() != null ? owner.getShopName()
                : owner.getFirstName() + " " + owner.getLastName()).orElse(null);
    }

    private String resolveCouponCode(Long couponId) {
        if (couponId == null) {
            return null;
        }
        Optional<SubscriptionCoupon> coupon = couponRepository.findById(couponId);
        return coupon.map(SubscriptionCoupon::getCode).orElse(null);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
