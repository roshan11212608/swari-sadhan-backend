package swari.sewa.module.expense.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.expense.entity.ExpenseCategory;
import swari.sewa.module.expense.service.ExpenseCategoryService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseCategory>>> getAllCategories() {
        List<ExpenseCategory> categories = expenseCategoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ExpenseCategory>>> getActiveCategories() {
        List<ExpenseCategory> categories = expenseCategoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategory>> getCategoryById(@PathVariable Long id) {
        Optional<ExpenseCategory> category = expenseCategoryService.getCategoryById(id);
        return category.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                      .orElse(ResponseEntity.ok(ApiResponse.error("Category not found")));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<ExpenseCategory>> getCategoryByName(@PathVariable String name) {
        Optional<ExpenseCategory> category = expenseCategoryService.getCategoryByName(name);
        return category.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                      .orElse(ResponseEntity.ok(ApiResponse.error("Category not found")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseCategory>> createCategory(@RequestBody ExpenseCategory category) {
        // Check if category with same name already exists
        Optional<ExpenseCategory> existing = expenseCategoryService.getCategoryByName(category.getName());
        if (existing.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(existing.get()));
        }
        // Set defaults if not provided
        if (category.getColor() == null || category.getColor().isBlank()) {
            category.setColor("#f97316");
        }
        if (category.getIcon() == null || category.getIcon().isBlank()) {
            category.setIcon("📁");
        }
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        ExpenseCategory saved = expenseCategoryService.createCategory(category);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }
}
