package swari.sewa.module.homepage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.homepage.entity.HomeFeaturedVehicle;

import java.util.List;

@Repository
public interface HomeFeaturedVehicleRepository extends JpaRepository<HomeFeaturedVehicle, Long> {
    List<HomeFeaturedVehicle> findByIsActiveTrueOrderByDisplayOrderAsc();
}
