package swari.sewa.module.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.vehicle.entity.PublicVehicleListingReviewHistory;

import java.util.List;

@Repository
public interface PublicVehicleListingReviewHistoryRepository extends JpaRepository<PublicVehicleListingReviewHistory, Long> {

    List<PublicVehicleListingReviewHistory> findByListingIdOrderByPerformedAtAsc(Long listingId);
}
