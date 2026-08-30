package swari.sewa.module.banner.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import swari.sewa.module.banner.entity.Banner;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Banner> findByPositionAndIsActiveTrue(String position);

    boolean existsByPosition(String position);
}
