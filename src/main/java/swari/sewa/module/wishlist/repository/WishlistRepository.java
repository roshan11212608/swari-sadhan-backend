package swari.sewa.module.wishlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.module.wishlist.entity.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    Page<Wishlist> findByCustomer_Id(Long customerId, Pageable pageable);
    
    List<Wishlist> findByCustomer_Id(Long customerId);
    
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wishlist w WHERE w.customer.id = :customerId AND w.vehicle.id = :vehicleId")
    boolean existsByCustomerIdAndVehicleId(@Param("customerId") Long customerId, @Param("vehicleId") Long vehicleId);
    
    @Query("SELECT w FROM Wishlist w WHERE w.customer.id = :customerId AND w.vehicle.id = :vehicleId")
    Optional<Wishlist> findByCustomerIdAndVehicleId(@Param("customerId") Long customerId, @Param("vehicleId") Long vehicleId);
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.customer.id = :customerId")
    Long countByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.vehicle.id = :vehicleId")
    Long countByVehicleId(@Param("vehicleId") Long vehicleId);
}
