package swari.sewa.module.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionTransactionRepository extends JpaRepository<SubscriptionTransaction, Long> {

    @Query("SELECT t FROM SubscriptionTransaction t WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:gateway IS NULL OR t.gateway = :gateway) " +
           "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
           "AND (:shopOwnerId IS NULL OR t.shopOwnerId = :shopOwnerId) " +
           "AND (:planId IS NULL OR t.plan.id = :planId) " +
           "AND (:fromDate IS NULL OR t.transactionDate >= :fromDate) " +
           "AND (:toDate IS NULL OR t.transactionDate <= :toDate)")
    Page<SubscriptionTransaction> findWithFilters(
            @Param("search") String search,
            @Param("status") TransactionStatus status,
            @Param("gateway") String gateway,
            @Param("paymentMethod") String paymentMethod,
            @Param("shopOwnerId") Long shopOwnerId,
            @Param("planId") Long planId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT t FROM SubscriptionTransaction t WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:gateway IS NULL OR t.gateway = :gateway) " +
           "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
           "AND (:shopOwnerId IS NULL OR t.shopOwnerId = :shopOwnerId) " +
           "AND (:planId IS NULL OR t.plan.id = :planId) " +
           "AND (:fromDate IS NULL OR t.transactionDate >= :fromDate) " +
           "AND (:toDate IS NULL OR t.transactionDate <= :toDate) " +
           "ORDER BY t.transactionDate DESC")
    List<SubscriptionTransaction> findForExport(
            @Param("search") String search,
            @Param("status") TransactionStatus status,
            @Param("gateway") String gateway,
            @Param("paymentMethod") String paymentMethod,
            @Param("shopOwnerId") Long shopOwnerId,
            @Param("planId") Long planId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COALESCE(SUM(t.finalAmount), 0) FROM SubscriptionTransaction t WHERE t.status = swari.sewa.module.subscription.enums.TransactionStatus.COMPLETED AND t.transactionDate >= :fromDate AND t.transactionDate <= :toDate")
    BigDecimal sumCompletedAmountBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COALESCE(SUM(t.finalAmount), 0) FROM SubscriptionTransaction t WHERE t.status = swari.sewa.module.subscription.enums.TransactionStatus.COMPLETED AND t.transactionDate >= :fromDate")
    BigDecimal sumCompletedAmountFrom(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, COUNT(t), COALESCE(SUM(t.finalAmount), 0) " +
           "FROM SubscriptionTransaction t WHERE t.status = swari.sewa.module.subscription.enums.TransactionStatus.COMPLETED AND t.transactionDate >= :fromDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') ORDER BY month")
    List<Object[]> getRevenueGrowthByMonth(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', s.createdAt, '%Y-%m') as month, COUNT(s) " +
           "FROM Subscription s WHERE s.createdAt >= :fromDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', s.createdAt, '%Y-%m') ORDER BY month")
    List<Object[]> getSubscriptionGrowthByMonth(@Param("fromDate") LocalDateTime fromDate);

    long countByStatus(TransactionStatus status);
}
