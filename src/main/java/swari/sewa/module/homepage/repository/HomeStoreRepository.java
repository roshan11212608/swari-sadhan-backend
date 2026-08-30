package swari.sewa.module.homepage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.homepage.entity.HomeStore;

import java.util.List;

@Repository
public interface HomeStoreRepository extends JpaRepository<HomeStore, Long> {
    List<HomeStore> findByIsActiveTrueOrderByDisplayOrderAsc();
}
