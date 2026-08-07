package swari.sewa.module.shop.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.shop.entity.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    
    Optional<Shop> findByLicenseNumber(String licenseNumber);
    
    @Query("SELECT s FROM Shop s WHERE s.user.email = :email")
    Optional<Shop> findByUserEmail(@Param("email") String email);

    @Query("SELECT s FROM Shop s WHERE s.shopOwner.email = :email")
    Optional<Shop> findByShopOwnerEmail(@Param("email") String email);

    @Query("SELECT s.id FROM Shop s WHERE s.user.email = :email")
    Optional<Long> findShopIdByUserEmail(@Param("email") String email);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    List<Shop> findByShopOwnerId(Long shopOwnerId);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.shopOwner.id = :shopOwnerId")
    List<Shop> findByShopOwnerIdWithShopOwner(@Param("shopOwnerId") Long shopOwnerId);
    
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
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user")
    List<Shop> findAllWithShopOwner();
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user")
    org.springframework.data.domain.Page<Shop> findAllWithShopOwner(org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.id = :id")
    Optional<Shop> findByIdWithShopOwner(@Param("id") Long id);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.city = :city AND s.status = 'ACTIVE'")
    List<Shop> findByCityAndStatusActiveWithShopOwner(@Param("city") String city);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.city = :city AND s.status = 'ACTIVE'")
    org.springframework.data.domain.Page<Shop> findByCityAndStatusActiveWithShopOwner(@Param("city") String city, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.state = :state AND s.status = 'ACTIVE'")
    List<Shop> findByStateAndStatusActiveWithShopOwner(@Param("state") String state);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.state = :state AND s.status = 'ACTIVE'")
    org.springframework.data.domain.Page<Shop> findByStateAndStatusActiveWithShopOwner(@Param("state") String state, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.isFeatured = true AND s.status = 'ACTIVE'")
    List<Shop> findFeaturedShopsWithShopOwner();
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.isFeatured = true AND s.status = 'ACTIVE'")
    org.springframework.data.domain.Page<Shop> findFeaturedShopsWithShopOwner(org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.name LIKE %:keyword% OR s.description LIKE %:keyword%")
    List<Shop> searchByKeywordWithShopOwner(@Param("keyword") String keyword);
    
    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.shopOwner LEFT JOIN FETCH s.user WHERE s.name LIKE %:keyword% OR s.description LIKE %:keyword%")
    org.springframework.data.domain.Page<Shop> searchByKeywordWithShopOwner(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}
