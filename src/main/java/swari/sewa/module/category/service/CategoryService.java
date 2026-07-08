package swari.sewa.module.category.service;

import java.util.List;
import java.util.Optional;

import swari.sewa.module.category.dto.CategoryDto;

public interface CategoryService {
    
    CategoryDto createCategory(CategoryDto categoryDto);
    
    Optional<CategoryDto> getCategoryById(Long id);
    
    Optional<CategoryDto> getCategoryByName(String name);
    
    List<CategoryDto> getAllCategories();
    
    List<CategoryDto> getActiveCategories();
    
    CategoryDto updateCategory(Long id, CategoryDto categoryDto);
    
    void deleteCategory(Long id);
    
    CategoryDto activateCategory(Long id);
    
    CategoryDto deactivateCategory(Long id);
    
    boolean existsByName(String name);
}
