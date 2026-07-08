package swari.sewa.module.vehicle.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.vehicle.entity.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    Page<Vehicle> findByShopId(Long shopId, Pageable pageable);
    
    Optional<Vehicle> findByIdAndShopId(Long id, Long shopId);
    
    Page<Vehicle> findByCategoryId(Long categoryId, Pageable pageable);
    
    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);
    
    Page<Vehicle> findByVehicleType(VehicleType vehicleType, Pageable pageable);
    
    Page<Vehicle> findByType(VehicleType type, Pageable pageable);
    
    Page<Vehicle> findByShop_ShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId, Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.shopOwner.id = :shopOwnerId")
    long countByShop_ShopOwner_Id(@Param("shopOwnerId") Long shopOwnerId);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.shopOwner.id = :shopOwnerId AND v.status = :status")
    long countByShop_ShopOwner_IdAndStatus(@Param("shopOwnerId") Long shopOwnerId, @Param("status") VehicleStatus status);
    
    Page<Vehicle> findByIsFeaturedTrue(Pageable pageable);
    
    boolean existsByRegistrationNumber(String registrationNumber);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'ACTIVE' AND v.shop.status = 'ACTIVE'")

    Page<Vehicle> findActiveVehicles(Pageable pageable);
    
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'ACTIVE' AND v.shop.status = 'ACTIVE' AND v.isFeatured = true")
    Page<Vehicle> findFeaturedVehicles(Pageable pageable);
    
    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:brand IS NULL OR v.brandName = :brand) AND " +
           "(:model IS NULL OR v.modelName = :model) AND " +
           "(:vehicleType IS NULL OR v.vehicleType = :vehicleType) AND " +
           "(:fuelType IS NULL OR v.fuelType = :fuelType) AND " +
           "(:minPrice IS NULL OR v.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR v.price <= :maxPrice) AND " +
           "(:minYear IS NULL OR v.manufacturingYear >= :minYear) AND " +
           "(:maxYear IS NULL OR v.manufacturingYear <= :maxYear) AND " +
           "(:minKilometers IS NULL OR v.kilometersDriven >= :minKilometers) AND " +
           "(:maxKilometers IS NULL OR v.kilometersDriven <= :maxKilometers) AND " +
           "(:city IS NULL OR v.shop.city = :city) AND " +
           "v.status = 'ACTIVE' AND v.shop.status = 'ACTIVE'")
    Page<Vehicle> searchVehicles(@Param("brand") String brand,
                               @Param("model") String model,
                               @Param("vehicleType") VehicleType vehicleType,
                               @Param("fuelType") String fuelType,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               @Param("minYear") Integer minYear,
                               @Param("maxYear") Integer maxYear,
                               @Param("minKilometers") Integer minKilometers,
                               @Param("maxKilometers") Integer maxKilometers,
                               @Param("city") String city,
                               Pageable pageable);
    
    @Query("SELECT v FROM Vehicle v WHERE v.title LIKE %:keyword% OR v.description LIKE %:keyword% OR v.brandName LIKE %:keyword% OR v.modelName LIKE %:keyword%")
    Page<Vehicle> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    Page<Vehicle> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status")
    Long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") VehicleStatus status);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status")
    Long countByStatus(@Param("status") VehicleStatus status);
    
    List<Vehicle> findByShopIdAndStatus(Long shopId, VehicleStatus status);
    
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'PENDING_APPROVAL'")
    List<Vehicle> findPendingApprovalVehicles();
}
