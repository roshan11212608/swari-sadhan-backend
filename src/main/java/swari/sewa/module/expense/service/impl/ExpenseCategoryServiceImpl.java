package swari.sewa.module.expense.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.expense.entity.ExpenseCategory;
import swari.sewa.module.expense.repository.ExpenseCategoryRepository;
import swari.sewa.module.expense.service.ExpenseCategoryService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategory> getAllCategories() {
        return expenseCategoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategory> getActiveCategories() {
        return expenseCategoryRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExpenseCategory> getCategoryById(Long id) {
        return expenseCategoryRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExpenseCategory> getCategoryByName(String name) {
        return expenseCategoryRepository.findByName(name);
    }

    @Override
    public ExpenseCategory createCategory(ExpenseCategory category) {
        return expenseCategoryRepository.save(category);
    }
}
