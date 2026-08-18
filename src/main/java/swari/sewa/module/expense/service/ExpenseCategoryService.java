package swari.sewa.module.expense.service;

import swari.sewa.module.expense.entity.ExpenseCategory;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryService {
    
    List<ExpenseCategory> getAllCategories();
    
    List<ExpenseCategory> getActiveCategories();
    
    Optional<ExpenseCategory> getCategoryById(Long id);
    
    Optional<ExpenseCategory> getCategoryByName(String name);
    
    ExpenseCategory createCategory(ExpenseCategory category);
}
