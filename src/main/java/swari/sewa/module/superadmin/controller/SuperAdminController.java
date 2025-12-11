package swari.sewa.module.superadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.superadmin.dto.DashboardStatsDto;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;
import swari.sewa.module.superadmin.dto.UserManagementDto;
import swari.sewa.module.superadmin.service.SuperAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getDashboardStats()));
    }

    @GetMapping("/shop-owners/list")
    public ResponseEntity<ApiResponse<Page<ShopOwnerDto>>> getShopOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getShopOwners(pageable)));
    }

    @PostMapping("/shop-owners/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateShopOwner(@PathVariable Long id) {
        superAdminService.activateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner activated successfully"));
    }

    @PostMapping("/shop-owners/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateShopOwner(@PathVariable Long id) {
        superAdminService.deactivateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner deactivated successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserManagementDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getUsers(pageable)));
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateUser(@PathVariable Long id) {
        superAdminService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully"));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateUser(@PathVariable Long id) {
        superAdminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<Object>> getReportsSummary() {
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getReportsSummary()));
    }

    @GetMapping("/reports/vehicles")
    public ResponseEntity<ApiResponse<Page<Object>>> getVehicleReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getVehicleReports(pageable)));
    }

    @GetMapping("/reports/enquiries")
    public ResponseEntity<ApiResponse<Page<Object>>> getEnquiryReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(superAdminService.getEnquiryReports(pageable)));
    }
}
