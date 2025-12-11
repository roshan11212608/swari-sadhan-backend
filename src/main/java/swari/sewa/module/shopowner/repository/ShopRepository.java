package swari.sewa.module.shopowner.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.shopowner.model.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    
    Optional<Shop> findByLicenseNumber(String licenseNumber);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    List<Shop> findByUserId(Long userId);
    
    @Query("SELECT s FROM Shop s WHERE s.shopOwner.id = :shopOwnerId")
    Page<Shop> findByShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.shopOwner.id = :shopOwnerId")
    long countByShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId);
    
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.shopOwner.id = :shopOwnerId AND s.status = :status")
    long countByShopOwner_IdAndStatus(@Param("shopOwnerId") Long shopOwnerId, @Param("status") ShopStatus status);
    
    List<Shop> findByStatus(ShopStatus status);
    
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);
    
    Page<Shop> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address, Pageable pageable);
    
    @Query("SELECT s FROM Shop s WHERE s.city = :city AND s.status = 'ACTIVE'")
    List<Shop> findByCityAndStatusActive(@Param("city") String city);
    
    @Query("SELECT s FROM Shop s WHERE s.state = :state AND s.status = 'ACTIVE'")
    List<Shop> findByStateAndStatusActive(@Param("state") String state);
    
    @Query("SELECT s FROM Shop s WHERE s.isFeatured = true AND s.status = 'ACTIVE'")
    List<Shop> findFeaturedShops();
    
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.status = :status")
    Long countByStatus(@Param("status") ShopStatus status);
    
    @Query("SELECT s FROM Shop s WHERE s.name LIKE %:keyword% OR s.description LIKE %:keyword%")
    List<Shop> searchByKeyword(@Param("keyword") String keyword);
}
