package swari.sewa.module.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.expense.entity.ExpenseCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    
    Optional<ExpenseCategory> findByName(String name);
    
    boolean existsByName(String name);
    
    List<ExpenseCategory> findByIsActiveTrueOrderByNameAsc();
}
