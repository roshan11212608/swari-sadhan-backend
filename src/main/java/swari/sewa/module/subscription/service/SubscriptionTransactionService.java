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
}
