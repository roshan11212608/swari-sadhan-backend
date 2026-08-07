package swari.sewa.module.vehicle.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.vehicle.entity.PublicVehicleListingSequence;

import java.util.Optional;

@Repository
public interface PublicVehicleListingSequenceRepository extends JpaRepository<PublicVehicleListingSequence, String> {

    /**
     * Acquires a pessimistic write lock on the sequence row for the given year-month,
     * preventing concurrent transactions from reading/updating it until the current
     * transaction commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PublicVehicleListingSequence s WHERE s.yearMonth = :yearMonth")
    Optional<PublicVehicleListingSequence> findByYearMonthForUpdate(@Param("yearMonth") String yearMonth);
}
