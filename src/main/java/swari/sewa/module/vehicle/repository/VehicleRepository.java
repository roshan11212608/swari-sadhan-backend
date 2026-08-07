package swari.sewa.module.vehicle.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    Page<Vehicle> findByStatusNot(VehicleStatus status, Pageable pageable);

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

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'INACTIVE' AND v.shop.status = 'ACTIVE'")
    Page<Vehicle> findInactiveVehicles(Pageable pageable);
    
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
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId")
    Long countByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT v.shop.id as shopId, COUNT(v) as count FROM Vehicle v GROUP BY v.shop.id")
    java.util.List<java.util.Map<String, Object>> countVehiclesByShopGrouped();
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status")
    Long countByStatus(@Param("status") VehicleStatus status);
    
    List<Vehicle> findByShopIdAndStatus(Long shopId, VehicleStatus status);
    
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'PENDING_APPROVAL'")
    List<Vehicle> findPendingApprovalVehicles();
    
    // Analytics Queries
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) BETWEEN :startDate AND :endDate)")
    Long countByShopIdAndBoughtDateBetween(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status AND v.soldAt BETWEEN :startDate AND :endDate")
    Long countByShopIdAndStatusAndSoldAtBetween(@Param("shopId") Long shopId, @Param("status") VehicleStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status IN :statuses")
    Long countByShopIdAndStatusIn(@Param("shopId") Long shopId, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query("SELECT COALESCE(SUM(v.price), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status AND v.soldAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPriceByShopIdAndStatusAndSoldAtBetween(@Param("shopId") Long shopId, @Param("status") VehicleStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(v.purchasePrice + COALESCE(v.additionalExpenses, 0)), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) BETWEEN :startDate AND :endDate)")
    BigDecimal sumPurchasePriceByShopIdAndBoughtDateBetween(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(v.purchasePrice + COALESCE(v.additionalExpenses, 0)), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status IN :statuses")
    BigDecimal sumPurchasePriceByShopIdAndStatusIn(@Param("shopId") Long shopId, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query("SELECT COALESCE(SUM(v.price - (COALESCE(v.purchasePrice, 0) + COALESCE(v.repairCost, 0) + COALESCE(v.additionalExpenses, 0))), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status AND v.soldAt BETWEEN :startDate AND :endDate")
    BigDecimal sumGrossProfitByShopIdAndStatusAndSoldAtBetween(@Param("shopId") Long shopId, @Param("status") VehicleStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Business Calculation Engine Queries
    
    @Query("SELECT COALESCE(SUM(COALESCE(v.purchasePrice, 0) + COALESCE(v.repairCost, 0) + COALESCE(v.additionalExpenses, 0)), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status AND v.soldAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCOGSByShopIdAndStatusAndSoldAtBetween(@Param("shopId") Long shopId, @Param("status") VehicleStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) > :date) AND v.status IN :statuses")
    Long countByShopIdAndBoughtDateAfterAndStatusIn(@Param("shopId") Long shopId, @Param("date") LocalDate date, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) BETWEEN :startDate AND :endDate) AND v.status IN :statuses")
    Long countByShopIdAndBoughtDateBetweenAndStatusIn(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) < :date) AND v.status IN :statuses")
    Long countByShopIdAndBoughtDateBeforeAndStatusIn(@Param("shopId") Long shopId, @Param("date") LocalDate date, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query("SELECT COALESCE(SUM(v.purchasePrice), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND (COALESCE(v.boughtDate, v.createdAt) < :date) AND v.status IN :statuses")
    BigDecimal sumPurchasePriceByShopIdAndBoughtDateBeforeAndStatusIn(@Param("shopId") Long shopId, @Param("date") LocalDate date, @Param("statuses") List<VehicleStatus> statuses);
    
    @Query(value = "SELECT " +
           "period, " +
           "COALESCE(SUM(sales), 0) as sales, " +
           "COALESCE(SUM(purchases), 0) as purchases " +
           "FROM (" +
           "  SELECT " +
           "  CASE WHEN :isYearly = true THEN CAST(YEAR(v.sold_at) AS CHAR) ELSE SUBSTRING(MONTHNAME(v.sold_at), 1, 3) END as period, " +
           "  CASE WHEN v.status = 'SOLD' AND v.sold_at BETWEEN :startDate AND :endDate THEN v.price ELSE 0 END as sales, " +
           "  CASE WHEN COALESCE(v.bought_date, v.created_at) BETWEEN :startDate AND :endDate THEN COALESCE(v.purchase_price, 0) + COALESCE(v.additional_expenses, 0) ELSE 0 END as purchases " +
           "  FROM vehicles v " +
           "  WHERE v.shop_id = :shopId " +
           "  AND (:isYearly = false OR YEAR(v.sold_at) = YEAR(:startDate))" +
           ") as subq " +
           "GROUP BY period " +
           "ORDER BY CASE WHEN :isYearly = true THEN period ELSE " +
           "CASE period WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4 " +
           "WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8 " +
           "WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12 END END", nativeQuery = true)
    List<Object[]> getSalesPurchaseTrend(@Param("shopId") Long shopId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("isYearly") Boolean isYearly);
    
    @Query(value = "SELECT " +
           "period, " +
           "COALESCE(SUM(sales), 0) as sales " +
           "FROM (" +
           "  SELECT " +
           "  CASE WHEN :isYearly = true THEN CAST(YEAR(v.sold_at) AS CHAR) ELSE SUBSTRING(MONTHNAME(v.sold_at), 1, 3) END as period, " +
           "  CASE WHEN v.status = 'SOLD' AND v.sold_at BETWEEN :startDate AND :endDate THEN v.price ELSE 0 END as sales " +
           "  FROM vehicles v " +
           "  WHERE v.shop_id = :shopId " +
           "  AND (:isYearly = false OR YEAR(v.sold_at) = YEAR(:startDate))" +
           ") as subq " +
           "GROUP BY period " +
           "ORDER BY CASE WHEN :isYearly = true THEN period ELSE " +
           "CASE period WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4 " +
           "WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8 " +
           "WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12 END END", nativeQuery = true)
    List<Object[]> getSalesTrend(@Param("shopId") Long shopId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("isYearly") Boolean isYearly);
    
    @Query(value = "SELECT " +
           "period, " +
           "COALESCE(SUM(grossProfit), 0) as grossProfit, " +
           "COALESCE(SUM(netProfit), 0) as netProfit " +
           "FROM (" +
           "  SELECT " +
           "  CASE WHEN :isYearly = true THEN CAST(YEAR(v.sold_at) AS CHAR) ELSE SUBSTRING(MONTHNAME(v.sold_at), 1, 3) END as period, " +
           "  CASE WHEN v.status = 'SOLD' AND v.sold_at BETWEEN :startDate AND :endDate THEN (v.price - (COALESCE(v.purchase_price, 0) + COALESCE(v.repair_cost, 0) + COALESCE(v.additional_expenses, 0))) ELSE 0 END as grossProfit, " +
           "  CASE WHEN v.status = 'SOLD' AND v.sold_at BETWEEN :startDate AND :endDate THEN (v.price - (COALESCE(v.purchase_price, 0) + COALESCE(v.repair_cost, 0) + COALESCE(v.additional_expenses, 0))) ELSE 0 END as netProfit " +
           "  FROM vehicles v " +
           "  WHERE v.shop_id = :shopId " +
           "  AND (:isYearly = false OR YEAR(v.sold_at) = YEAR(:startDate))" +
           ") as subq " +
           "GROUP BY period " +
           "ORDER BY CASE WHEN :isYearly = true THEN period ELSE " +
           "CASE period WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4 " +
           "WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8 " +
           "WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12 END END", nativeQuery = true)
    List<Object[]> getProfitTrend(@Param("shopId") Long shopId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("isYearly") Boolean isYearly);
    
    @Query(value = "SELECT " +
           "period, " +
           "COALESCE(SUM(moneyIn), 0) as moneyIn, " +
           "COALESCE(SUM(moneyOut), 0) as moneyOut " +
           "FROM (" +
           "  SELECT " +
           "  CASE WHEN :isYearly = true THEN CAST(YEAR(COALESCE(v.sold_at, v.bought_date, v.created_at)) AS CHAR) ELSE SUBSTRING(MONTHNAME(COALESCE(v.sold_at, v.bought_date, v.created_at)), 1, 3) END as period, " +
           "  CASE WHEN v.status = 'SOLD' AND v.sold_at BETWEEN :startDate AND :endDate THEN v.price ELSE 0 END as moneyIn, " +
           "  CASE WHEN COALESCE(v.bought_date, v.created_at) BETWEEN :startDate AND :endDate THEN COALESCE(v.purchase_price, 0) ELSE 0 END as moneyOut " +
           "  FROM vehicles v " +
           "  WHERE v.shop_id = :shopId " +
           "  AND (:isYearly = false OR YEAR(COALESCE(v.sold_at, v.bought_date, v.created_at)) = YEAR(:startDate))" +
           ") as subq " +
           "GROUP BY period " +
           "ORDER BY CASE WHEN :isYearly = true THEN period ELSE " +
           "CASE period WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4 " +
           "WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8 " +
           "WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12 END END", nativeQuery = true)
    List<Object[]> getCashFlowTrend(@Param("shopId") Long shopId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("isYearly") Boolean isYearly);
    
    @Query("SELECT COALESCE(SUM(v.price), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status")
    BigDecimal sumPriceByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") VehicleStatus status);
    
    @Query("SELECT COALESCE(SUM(v.purchasePrice), 0) FROM Vehicle v WHERE v.shop.id = :shopId")
    BigDecimal sumPurchasePriceByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT COALESCE(SUM(v.purchasePrice), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status")
    BigDecimal sumPurchasePriceByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);
    
    @Query("SELECT COALESCE(SUM(v.price), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status IN :statuses")
    BigDecimal sumPriceByShopIdAndStatusIn(@Param("shopId") Long shopId, @Param("statuses") List<VehicleStatus> statuses);
}
