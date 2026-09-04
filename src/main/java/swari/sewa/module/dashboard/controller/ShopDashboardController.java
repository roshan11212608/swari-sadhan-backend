package swari.sewa.module.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.dashboard.dto.ShopDashboardSummaryDto;
import swari.sewa.module.dashboard.service.DashboardService;
import swari.sewa.module.shop.repository.ShopRepository;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ShopDashboardController {

    private final DashboardService dashboardService;
    private final ShopRepository shopRepository;

    /**
     * Lightweight shop owner dashboard summary.
     * Returns KPI counts + 5 recent vehicles + 5 recent enquiries + review summary
     * in a single response, replacing 3 heavy frontend API calls
     * (500 vehicles, 20 enquiries, all reviews).
     */
    @GetMapping("/shop-summary")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<ShopDashboardSummaryDto>> getShopSummary(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopIdHeader,
            Authentication authentication) {

        Long shopId = shopIdHeader;

        // If no header, try resolving from authenticated shop owner's email
        if (shopId == null && authentication != null && authentication.getName() != null) {
            shopId = shopRepository.findShopIdByShopOwnerEmail(authentication.getName()).orElse(null);
        }

        if (shopId == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(dashboardService.getShopDashboardSummary(shopId)));
    }
}
