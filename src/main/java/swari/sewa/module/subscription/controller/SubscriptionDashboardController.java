package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.service.SubscriptionDashboardService;

@RestController
@RequestMapping("/api/superadmin/subscription/dashboard")
@RequiredArgsConstructor
public class SubscriptionDashboardController {

    private final SubscriptionDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "30d") String timeRange) {
        SubscriptionDashboardResponse dashboard = dashboardService.getDashboard(timeRange);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard data retrieved successfully"));
    }
}
