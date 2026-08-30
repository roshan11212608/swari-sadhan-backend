package swari.sewa.module.vehicle.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.common.enums.PublicVehicleListingStatus;
import swari.sewa.module.vehicle.entity.PublicVehicleListing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicVehicleListingRepository extends JpaRepository<PublicVehicleListing, Long> {

    Optional<PublicVehicleListing> findByListingNumber(String listingNumber);

    Optional<PublicVehicleListing> findByIdAndSellerUserId(Long id, Long sellerUserId);

    Page<PublicVehicleListing> findBySellerUserId(Long sellerUserId, Pageable pageable);

    Page<PublicVehicleListing> findBySellerUserIdAndStatus(Long sellerUserId, PublicVehicleListingStatus status, Pageable pageable);

    Page<PublicVehicleListing> findByStatus(PublicVehicleListingStatus status, Pageable pageable);

    Page<PublicVehicleListing> findByStatusIn(List<PublicVehicleListingStatus> statuses, Pageable pageable);

    @Query("SELECT p FROM PublicVehicleListing p WHERE p.status IN :statuses AND p.vehicleNumber = :vehicleNumber")
    List<PublicVehicleListing> findActiveByVehicleNumber(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("statuses") List<PublicVehicleListingStatus> statuses);

    @Query("SELECT COUNT(p) FROM PublicVehicleListing p WHERE p.status IN :statuses AND p.vehicleNumber = :vehicleNumber")
    long countActiveByVehicleNumber(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("statuses") List<PublicVehicleListingStatus> statuses);

    @Query("SELECT p FROM PublicVehicleListing p WHERE p.status = 'PUBLISHED' AND (p.soldAt IS NULL) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND (:city IS NULL OR p.sellerAddress LIKE CONCAT('%', :city, '%')) " +
           "ORDER BY p.publishedAt DESC")
    Page<PublicVehicleListing> findPublishedListings(
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("brand") String brand,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM PublicVehicleListing p WHERE p.status = :status")
    long countByStatus(@Param("status") PublicVehicleListingStatus status);

    @Query("SELECT COUNT(p) FROM PublicVehicleListing p WHERE p.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);
}
