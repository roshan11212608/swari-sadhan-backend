package swari.sewa.module.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.expense.entity.ExpenseAttachment;

import java.util.List;

@Repository
public interface ExpenseAttachmentRepository extends JpaRepository<ExpenseAttachment, Long> {
    
    List<ExpenseAttachment> findByExpenseId(Long expenseId);
    
    void deleteByExpenseId(Long expenseId);
}
