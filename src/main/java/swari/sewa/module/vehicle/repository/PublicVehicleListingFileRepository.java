package swari.sewa.module.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.vehicle.entity.PublicVehicleListingFile;

import java.util.List;

@Repository
public interface PublicVehicleListingFileRepository extends JpaRepository<PublicVehicleListingFile, Long> {
    List<PublicVehicleListingFile> findByListingId(Long listingId);
    List<PublicVehicleListingFile> findByListingIdAndIsPublicTrue(Long listingId);
    List<PublicVehicleListingFile> findByListingIdAndIsCoverTrue(Long listingId);
}
