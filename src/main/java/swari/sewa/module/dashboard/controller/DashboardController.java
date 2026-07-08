package swari.sewa.module.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.dashboard.dto.DashboardStatsDto;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.dashboard.dto.UserManagementDto;
import swari.sewa.module.dashboard.dto.CredentialsDto;
import swari.sewa.module.dashboard.service.DashboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats()));
    }

    @GetMapping("/shop-owners-management")
    public ResponseEntity<ApiResponse<Page<ShopOwnerDto>>> getShopOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getShopOwners(pageable)));
    }

    @PostMapping("/shop-owners/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateShopOwner(@PathVariable Long id) {
        dashboardService.activateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner activated successfully"));
    }

    @PostMapping("/shop-owners/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateShopOwner(@PathVariable Long id) {
        dashboardService.deactivateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner deactivated successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserManagementDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getUsers(pageable)));
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateUser(@PathVariable Long id) {
        dashboardService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully"));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateUser(@PathVariable Long id) {
        dashboardService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<Object>> getReportsSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getReportsSummary()));
    }

    @GetMapping("/reports/vehicles")
    public ResponseEntity<ApiResponse<Page<Object>>> getVehicleReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getVehicleReports(pageable)));
    }

    @GetMapping("/reports/enquiries")
    public ResponseEntity<ApiResponse<Page<Object>>> getEnquiryReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getEnquiryReports(pageable)));
    }

    // Credentials Management Endpoints
    @GetMapping("/credentials")
    public ResponseEntity<ApiResponse<Page<CredentialsDto>>> getCredentials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCredentials(pageable, userType, status, search)));
    }

    @GetMapping("/credentials/{id}")
    public ResponseEntity<ApiResponse<CredentialsDto>> getCredentialById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCredentialById(id)));
    }

    @PostMapping("/credentials/{id}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable Long id) {
        String newPassword = dashboardService.resetUserPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. New password: " + newPassword));
    }

    @PostMapping("/credentials/{id}/toggle-status")
    public ResponseEntity<ApiResponse<String>> toggleCredentialStatus(@PathVariable Long id) {
        dashboardService.toggleCredentialStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Credential status updated successfully"));
    }

    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCredential(@PathVariable Long id) {
        dashboardService.deleteCredential(id);
        return ResponseEntity.ok(ApiResponse.success("Credential deleted successfully"));
    }
}
