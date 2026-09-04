package swari.sewa.module.employee.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import swari.sewa.module.employee.entity.AdvancePayment;

@Repository
public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {
    
    List<AdvancePayment> findByEmployeeId(Long employeeId);
    
    Page<AdvancePayment> findByEmployeeId(Long employeeId, Pageable pageable);
    
    @Query("SELECT a FROM AdvancePayment a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND a.status = :status AND a.deletedAt IS NULL")
    List<AdvancePayment> findByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);
    
    @Query("SELECT a FROM AdvancePayment a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND a.status = 'Pending' AND a.deletedAt IS NULL")
    List<AdvancePayment> findPendingByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT a FROM AdvancePayment a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND (a.status = 'Pending' OR a.status = 'Partially Recovered') AND a.deletedAt IS NULL")
    List<AdvancePayment> findActiveByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT a FROM AdvancePayment a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND a.deletedAt IS NULL ORDER BY a.date DESC")
    List<AdvancePayment> findByShopIdOrderByDateDesc(@Param("shopId") Long shopId);
    
    @Query("SELECT SUM(a.advanceAmount) FROM AdvancePayment a WHERE a.employee.shop.id = :shopId AND a.deletedAt IS NULL")
    java.math.BigDecimal sumAdvanceAmountByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT SUM(a.recoveredAmount) FROM AdvancePayment a WHERE a.employee.shop.id = :shopId AND a.status = 'Fully Recovered' AND a.deletedAt IS NULL")
    java.math.BigDecimal sumRecoveredAmountByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT SUM(a.remainingBalance) FROM AdvancePayment a WHERE a.employee.shop.id = :shopId AND (a.status = 'Pending' OR a.status = 'Partially Recovered') AND a.deletedAt IS NULL")
    java.math.BigDecimal sumRemainingBalanceByShopId(@Param("shopId") Long shopId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdvancePayment a WHERE a.id = :id")
    java.util.Optional<AdvancePayment> findByIdWithLock(@Param("id") Long id);

    // ── Advance summary: count of pending requests (for KPI without loading ALL advances) ──

    @Query("SELECT COUNT(a) FROM AdvancePayment a WHERE a.employee.shop.id = :shopId AND a.status = :status AND a.deletedAt IS NULL")
    Long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);
}
