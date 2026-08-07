package swari.sewa.module.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import swari.sewa.module.employee.entity.SalaryRecord;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    
    List<SalaryRecord> findByEmployeeId(Long employeeId);
    
    Page<SalaryRecord> findByEmployeeId(Long employeeId, Pageable pageable);
    
    @Query("SELECT s FROM SalaryRecord s JOIN FETCH s.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND s.month = :month AND s.year = :year AND s.deletedAt IS NULL")
    List<SalaryRecord> findByShopIdAndMonthAndYear(@Param("shopId") Long shopId, @Param("month") int month, @Param("year") int year);
    
    @Query("SELECT s FROM SalaryRecord s WHERE s.employee.id = :employeeId AND s.month = :month AND s.year = :year AND s.deletedAt IS NULL")
    Optional<SalaryRecord> findByEmployeeIdAndMonthAndYear(@Param("employeeId") Long employeeId, @Param("month") int month, @Param("year") int year);
    
    @Query("SELECT s FROM SalaryRecord s JOIN FETCH s.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND s.paymentStatus = :status AND s.deletedAt IS NULL")
    List<SalaryRecord> findByShopIdAndPaymentStatus(@Param("shopId") Long shopId, @Param("status") String status);
    
    @Query("SELECT s FROM SalaryRecord s JOIN FETCH s.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND s.paymentStatus = 'Pending' AND s.deletedAt IS NULL")
    List<SalaryRecord> findPendingByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT SUM(s.netSalary) FROM SalaryRecord s WHERE s.employee.shop.id = :shopId AND s.month = :month AND s.year = :year AND s.deletedAt IS NULL")
    java.math.BigDecimal sumNetSalaryByShopIdAndMonthAndYear(@Param("shopId") Long shopId, @Param("month") int month, @Param("year") int year);
    
    @Query("SELECT SUM(s.netSalary) FROM SalaryRecord s WHERE s.employee.shop.id = :shopId AND s.month = :month AND s.year = :year AND s.paymentStatus = 'Paid' AND s.deletedAt IS NULL")
    java.math.BigDecimal sumPaidSalaryByShopIdAndMonthAndYear(@Param("shopId") Long shopId, @Param("month") int month, @Param("year") int year);
    
    @Query("SELECT COALESCE(s.balanceDue, 0) FROM SalaryRecord s WHERE s.employee.id = :employeeId AND (s.year < :year OR (s.year = :year AND s.month < :month)) AND s.deletedAt IS NULL ORDER BY s.year DESC, s.month DESC LIMIT 1")
    java.math.BigDecimal getLatestPreviousBalanceDueByEmployeeId(@Param("employeeId") Long employeeId, @Param("year") int year, @Param("month") int month);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SalaryRecord s WHERE s.id = :id")
    Optional<SalaryRecord> findByIdWithLock(@Param("id") Long id);
}
