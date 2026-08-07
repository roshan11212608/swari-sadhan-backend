package swari.sewa.module.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.analytics.dto.AnalyticsDashboardResponse;
import swari.sewa.module.analytics.service.AnalyticsService;
import swari.sewa.module.shop.repository.ShopRepository;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ShopRepository shopRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "analyticsDashboard", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<AnalyticsDashboardResponse>> getDashboard(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        
        // Derive shop from authenticated user
        Long shopId = shopRepository.findShopIdByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Shop not found for user: " + userEmail));
        
        System.out.println("=== ANALYTICS DASHBOARD CALLED ===");
        System.out.println("User: " + userEmail);
        System.out.println("Shop ID: " + shopId);
        System.out.println("Filter: " + filter);
        
        AnalyticsDashboardResponse dashboard = analyticsService.getDashboard(shopId, filter);
        
        System.out.println("Vehicles Purchased: " + dashboard.getBusinessOverview().getKpi().getVehiclesPurchased());
        
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Analytics dashboard loaded successfully"));
    }
}
