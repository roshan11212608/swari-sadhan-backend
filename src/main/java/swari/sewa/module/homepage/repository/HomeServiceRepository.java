package swari.sewa.module.homepage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.homepage.entity.HomeService;

import java.util.List;

@Repository
public interface HomeServiceRepository extends JpaRepository<HomeService, Long> {
    List<HomeService> findByIsActiveTrueOrderByDisplayOrderAsc();
}
