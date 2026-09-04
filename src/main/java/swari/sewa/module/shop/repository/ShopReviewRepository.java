package swari.sewa.module.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.shop.entity.ShopReview;

import java.util.List;

@Repository
public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    List<ShopReview> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<ShopReview> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    long countByShopId(Long shopId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ShopReview r WHERE r.shopId = :shopId")
    double getAverageRatingByShopId(@Param("shopId") Long shopId);

    /**
     * Batch query: returns review count and average rating for multiple shops
     * in a single query, eliminating N+1 when mapping a list of shops.
     * Each row is [shopId, count, avgRating].
     */
    @Query("SELECT r.shopId, COUNT(r), COALESCE(AVG(r.rating), 0.0) " +
           "FROM ShopReview r WHERE r.shopId IN :shopIds " +
           "GROUP BY r.shopId")
    List<Object[]> countAndAvgRatingByShopIds(@Param("shopIds") List<Long> shopIds);

    /**
     * Aggregate review count and weighted total rating across ALL shops owned
     * by a given shop owner, in a single query.
     * Returns [totalReviewCount, weightedRatingSum] where weightedRatingSum is
     * the sum of all individual ratings (so avg = weightedRatingSum / totalReviewCount).
     * Eliminates the N+1 loop over shops in getDashboardStats().
     */
    @Query("SELECT COUNT(r), COALESCE(SUM(r.rating), 0) " +
           "FROM ShopReview r, Shop s " +
           "WHERE r.shopId = s.id AND s.shopOwner.id = :shopOwnerId")
    Object[] aggregateRatingByShopOwnerId(@Param("shopOwnerId") Long shopOwnerId);

    // ── Paginated + filtered queries for review list endpoints ──

    long countByShopIdAndRating(Long shopId, Integer rating);

    @Query("SELECT r FROM ShopReview r WHERE r.shopId = :shopId " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:search IS NULL OR LOWER(r.reviewerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<ShopReview> findByShopIdWithFilters(
            @Param("shopId") Long shopId,
            @Param("rating") Integer rating,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM ShopReview r WHERE r.shopId = :shopId " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:search IS NULL OR LOWER(r.reviewerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :search, '%')))")
    long countByShopIdWithFilters(
            @Param("shopId") Long shopId,
            @Param("rating") Integer rating,
            @Param("search") String search);

    @Query("SELECT r.rating, COUNT(r) FROM ShopReview r WHERE r.shopId = :shopId " +
           "GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> countByShopIdGroupByRating(@Param("shopId") Long shopId);
}
