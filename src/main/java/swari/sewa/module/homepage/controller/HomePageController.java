package swari.sewa.module.homepage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.homepage.dto.*;
import swari.sewa.module.homepage.service.HomePageService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class HomePageController {

    private final HomePageService homePageService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<HomePageDto>> getHomePage() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getHomePage()));
    }

    // Brand admin endpoints
    @GetMapping("/admin/home/brands")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<HomeBrandDto>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getAllBrands()));
    }

    @PostMapping("/admin/home/brands")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeBrandDto>> createBrand(@RequestBody HomeBrandDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.createBrand(dto), "Brand created successfully"));
    }

    @PutMapping("/admin/home/brands/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeBrandDto>> updateBrand(@PathVariable Long id, @RequestBody HomeBrandDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.updateBrand(id, dto), "Brand updated successfully"));
    }

    @DeleteMapping("/admin/home/brands/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        homePageService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Brand deleted successfully"));
    }

    // Featured vehicle admin endpoints
    @GetMapping("/admin/home/featured-vehicles")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<HomeFeaturedVehicleDto>>> getAllFeaturedVehicles() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getAllFeaturedVehicles()));
    }

    @PostMapping("/admin/home/featured-vehicles")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeFeaturedVehicleDto>> createFeaturedVehicle(@RequestBody HomeFeaturedVehicleDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.createFeaturedVehicle(dto), "Featured vehicle created successfully"));
    }

    @PutMapping("/admin/home/featured-vehicles/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeFeaturedVehicleDto>> updateFeaturedVehicle(@PathVariable Long id, @RequestBody HomeFeaturedVehicleDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.updateFeaturedVehicle(id, dto), "Featured vehicle updated successfully"));
    }

    @DeleteMapping("/admin/home/featured-vehicles/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFeaturedVehicle(@PathVariable Long id) {
        homePageService.deleteFeaturedVehicle(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Featured vehicle deleted successfully"));
    }

    // Budget admin endpoints
    @GetMapping("/admin/home/budgets")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<HomeBudgetDto>>> getAllBudgets() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getAllBudgets()));
    }

    @PostMapping("/admin/home/budgets")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeBudgetDto>> createBudget(@Valid @RequestBody HomeBudgetDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.createBudget(dto), "Budget created successfully"));
    }

    @PutMapping("/admin/home/budgets/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeBudgetDto>> updateBudget(@PathVariable Long id, @Valid @RequestBody HomeBudgetDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.updateBudget(id, dto), "Budget updated successfully"));
    }

    @DeleteMapping("/admin/home/budgets/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        homePageService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Budget deleted successfully"));
    }

    // Service admin endpoints
    @GetMapping("/admin/home/services")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<HomeServiceDto>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getAllServices()));
    }

    @PostMapping("/admin/home/services")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeServiceDto>> createService(@RequestBody HomeServiceDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.createService(dto), "Service created successfully"));
    }

    @PutMapping("/admin/home/services/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeServiceDto>> updateService(@PathVariable Long id, @RequestBody HomeServiceDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.updateService(id, dto), "Service updated successfully"));
    }

    @DeleteMapping("/admin/home/services/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        homePageService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Service deleted successfully"));
    }

    // Stores admin endpoints
    @GetMapping("/admin/home/stores")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<HomeStoreDto>>> getAllStores() {
        return ResponseEntity.ok(ApiResponse.success(homePageService.getAllStores()));
    }

    @PostMapping("/admin/home/stores")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeStoreDto>> createStore(@Valid @RequestBody HomeStoreDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.createStore(dto), "Store created successfully"));
    }

    @PutMapping("/admin/home/stores/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<HomeStoreDto>> updateStore(@PathVariable Long id, @Valid @RequestBody HomeStoreDto dto) {
        return ResponseEntity.ok(ApiResponse.success(homePageService.updateStore(id, dto), "Store updated successfully"));
    }

    @DeleteMapping("/admin/home/stores/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable Long id) {
        homePageService.deleteStore(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Store deleted successfully"));
    }
}
