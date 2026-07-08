package swari.sewa.module.vehicle.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.vehicle.entity.SellVehicleApplication;
import swari.sewa.common.enums.ApplicationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellVehicleApplicationRepository extends JpaRepository<SellVehicleApplication, Long> {

    Optional<SellVehicleApplication> findByIdAndShopId(Long id, Long shopId);

    Page<SellVehicleApplication> findByShopId(Long shopId, Pageable pageable);

    Page<SellVehicleApplication> findByShopIdAndStatus(Long shopId, ApplicationStatus status, Pageable pageable);

    Page<SellVehicleApplication> findByVehicleId(Long vehicleId, Pageable pageable);

    List<SellVehicleApplication> findByVehicleId(Long vehicleId);

    List<SellVehicleApplication> findByVehicleIdAndShopId(Long vehicleId, Long shopId);

    @Query("SELECT COUNT(s) FROM SellVehicleApplication s WHERE s.shop.id = :shopId")
    long countByShopId(@Param("shopId") Long shopId);

    @Query("SELECT COUNT(s) FROM SellVehicleApplication s WHERE s.shop.id = :shopId AND s.status = :status")
    long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") ApplicationStatus status);

    @Query("SELECT s FROM SellVehicleApplication s WHERE s.shop.id = :shopId AND s.customerEmail LIKE %:email%")
    Page<SellVehicleApplication> findByShopIdAndCustomerEmailContaining(@Param("shopId") Long shopId, @Param("email") String email, Pageable pageable);

    @Query("SELECT s FROM SellVehicleApplication s WHERE s.shop.id = :shopId AND s.customerPhone LIKE %:phone%")
    Page<SellVehicleApplication> findByShopIdAndCustomerPhoneContaining(@Param("shopId") Long shopId, @Param("phone") String phone, Pageable pageable);

    @Query("SELECT s FROM SellVehicleApplication s WHERE s.shop.id = :shopId AND s.customerName LIKE %:name%")
    Page<SellVehicleApplication> findByShopIdAndCustomerNameContaining(@Param("shopId") Long shopId, @Param("name") String name, Pageable pageable);
}
