package swari.sewa.module.employee.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.module.employee.entity.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByEmployeeId(Long employeeId);
    
    Page<Attendance> findByEmployeeId(Long employeeId, Pageable pageable);
    
    List<Attendance> findByDate(LocalDate date);
    
    Page<Attendance> findByDate(LocalDate date, Pageable pageable);
    
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND a.date = :date AND a.deletedAt IS NULL")
    List<Attendance> findByShopIdAndDate(@Param("shopId") Long shopId, @Param("date") LocalDate date);
    
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND " +
           "YEAR(a.date) = :year AND MONTH(a.date) = :month AND a.deletedAt IS NULL")
    List<Attendance> findByShopIdAndMonth(@Param("shopId") Long shopId, @Param("year") int year, @Param("month") int month);
    
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND " +
           "YEAR(a.date) = :year AND MONTH(a.date) = :month AND a.deletedAt IS NULL")
    List<Attendance> findByEmployeeIdAndMonthAndYear(@Param("employeeId") Long employeeId, @Param("year") int year, @Param("month") int month);
    
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND a.date = :date AND a.deletedAt IS NULL")
    Optional<Attendance> findByEmployeeIdAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(a) FROM Attendance a JOIN a.employee e JOIN e.shop WHERE e.shop.id = :shopId AND a.date = :date AND a.status = :status AND a.deletedAt IS NULL")
    Long countByShopIdAndDateAndStatus(@Param("shopId") Long shopId, @Param("date") LocalDate date, @Param("status") String status);

    // ── Attendance trend: single GROUP BY query for 7 months (replaces 7 monthly queries) ──

    @Query("SELECT YEAR(a.date), MONTH(a.date), a.status, COUNT(a) FROM Attendance a " +
           "JOIN a.employee e JOIN e.shop WHERE e.shop.id = :shopId " +
           "AND a.date >= :startDate AND a.deletedAt IS NULL " +
           "GROUP BY YEAR(a.date), MONTH(a.date), a.status " +
           "ORDER BY YEAR(a.date), MONTH(a.date)")
    List<Object[]> countByShopIdAndDateRangeGroupByMonthAndStatus(
            @Param("shopId") Long shopId,
            @Param("startDate") LocalDate startDate);
}
