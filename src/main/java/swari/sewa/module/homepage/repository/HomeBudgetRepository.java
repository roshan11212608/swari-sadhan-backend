package swari.sewa.module.homepage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.homepage.entity.HomeBudget;

import java.util.List;

@Repository
public interface HomeBudgetRepository extends JpaRepository<HomeBudget, Long> {
    List<HomeBudget> findByIsActiveTrueOrderByDisplayOrderAsc();
}
