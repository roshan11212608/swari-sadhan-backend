package swari.sewa.module.employee.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.module.employee.entity.LeaveRequest;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    
    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND l.status = :status AND l.deletedAt IS NULL")
    List<LeaveRequest> findByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);
    
    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND l.status = 'Pending' AND l.deletedAt IS NULL")
    List<LeaveRequest> findPendingByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.employee e JOIN FETCH e.shop WHERE e.shop.id = :shopId AND l.deletedAt IS NULL ORDER BY l.appliedDate DESC")
    List<LeaveRequest> findByShopIdOrderByAppliedDateDesc(@Param("shopId") Long shopId);
    
    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id = :employeeId AND l.status = :status AND l.deletedAt IS NULL")
    List<LeaveRequest> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId, @Param("status") String status);
}
