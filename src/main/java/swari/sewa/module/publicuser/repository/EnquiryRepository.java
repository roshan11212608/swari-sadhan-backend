package swari.sewa.module.publicuser.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.publicuser.model.Enquiry;

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
}
