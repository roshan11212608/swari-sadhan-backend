package swari.sewa.module.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.shop.entity.ShopReview;

import java.util.List;

@Repository
public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    List<ShopReview> findByShopIdOrderByCreatedAtDesc(Long shopId);

    long countByShopId(Long shopId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ShopReview r WHERE r.shopId = :shopId")
    double getAverageRatingByShopId(@Param("shopId") Long shopId);
}
