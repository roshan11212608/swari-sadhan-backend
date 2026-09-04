package swari.sewa.module.enquiry.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.enquiry.entity.Enquiry;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    Page<Enquiry> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e WHERE e.shop.shopOwner.id = :shopOwnerId")
    Page<Enquiry> findByShop_ShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.shop.shopOwner.id = :shopOwnerId")
    long countByShop_ShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.shop.shopOwner.id = :shopOwnerId AND e.status = :status")
    long countByShop_ShopOwner_IdAndStatus(@Param("shopOwnerId") Long shopOwnerId, @Param("status") EnquiryStatus status);

    Page<Enquiry> findByShopId(Long shopId, Pageable pageable);

    Page<Enquiry> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<Enquiry> findByStatus(EnquiryStatus status, Pageable pageable);

    List<Enquiry> findByShopIdAndStatus(Long shopId, EnquiryStatus status);

    List<Enquiry> findByCustomerIdAndStatus(Long customerId, EnquiryStatus status);

    @Query("SELECT e FROM Enquiry e WHERE e.shop.id = :shopId AND e.status = :status")
    List<Enquiry> findByShopIdAndStatusList(@Param("shopId") Long shopId, @Param("status") EnquiryStatus status);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.shop.id = :shopId AND e.status = :status")
    Long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") EnquiryStatus status);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.customer.id = :customerId AND e.status = :status")
    Long countByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") EnquiryStatus status);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.status = :status")
    Long countByStatus(@Param("status") EnquiryStatus status);

    @Query("SELECT e FROM Enquiry e WHERE e.customerEmail LIKE %:keyword% OR e.customerName LIKE %:keyword% OR e.message LIKE %:keyword%")
    Page<Enquiry> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Enquiry> findByCustomerFirstNameContainingIgnoreCaseOrCustomerLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    // ── JOIN FETCH variants (eliminate N+1 on customer/vehicle/shop) ──

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop")
    Page<Enquiry> findAllWithCustomerVehicleShop(Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.customer.id = :customerId")
    Page<Enquiry> findByCustomerIdWithCustomerVehicleShop(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.shop.id = :shopId")
    Page<Enquiry> findByShopIdWithCustomerVehicleShop(@Param("shopId") Long shopId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.vehicle.id = :vehicleId")
    Page<Enquiry> findByVehicleIdWithCustomerVehicleShop(@Param("vehicleId") Long vehicleId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.status = :status")
    Page<Enquiry> findByStatusWithCustomerVehicleShop(@Param("status") EnquiryStatus status, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.shop.id = :shopId AND e.status = :status")
    List<Enquiry> findByShopIdAndStatusWithCustomerVehicleShop(@Param("shopId") Long shopId, @Param("status") EnquiryStatus status);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.customerEmail LIKE %:keyword% OR e.customerName LIKE %:keyword% OR e.message LIKE %:keyword%")
    Page<Enquiry> searchByKeywordWithCustomerVehicleShop(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE " +
           "e.customer.firstName LIKE %:firstName% OR e.customer.lastName LIKE %:lastName%")
    Page<Enquiry> searchByCustomerNameWithCustomerVehicleShop(@Param("firstName") String firstName, @Param("lastName") String lastName, Pageable pageable);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.id = :id")
    Optional<Enquiry> findByIdWithCustomerVehicleShop(@Param("id") Long id);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.customer JOIN FETCH e.vehicle JOIN FETCH e.shop WHERE e.shop.shopOwner.id = :shopOwnerId")
    Page<Enquiry> findByShopOwner_IdWithCustomerVehicleShop(@Param("shopOwnerId") Long shopOwnerId, Pageable pageable);

    // ── Lightweight projection queries for EnquirySecurity (avoid loading full entity) ──

    @Query("SELECT e.customer.email FROM Enquiry e WHERE e.id = :id")
    Optional<String> findCustomerEmailById(@Param("id") Long id);

    @Query("SELECT e.shop.shopOwner.email FROM Enquiry e WHERE e.id = :id")
    Optional<String> findShopOwnerEmailById(@Param("id") Long id);
}
