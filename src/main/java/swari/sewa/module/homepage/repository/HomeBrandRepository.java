package swari.sewa.module.homepage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.homepage.entity.HomeBrand;

import java.util.List;

@Repository
public interface HomeBrandRepository extends JpaRepository<HomeBrand, Long> {
    List<HomeBrand> findByIsActiveTrueOrderByDisplayOrderAsc();
}
