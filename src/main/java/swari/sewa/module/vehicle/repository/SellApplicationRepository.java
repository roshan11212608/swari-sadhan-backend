package swari.sewa.module.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.vehicle.entity.SellApplication;

import java.util.List;

@Repository
public interface SellApplicationRepository extends JpaRepository<SellApplication, Long> {
    List<SellApplication> findByVehicleId(Long vehicleId);
}
