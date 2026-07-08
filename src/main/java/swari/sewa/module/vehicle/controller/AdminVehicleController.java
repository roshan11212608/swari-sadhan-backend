package swari.sewa.module.vehicle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.vehicle.service.AdminVehicleService;

@RestController
@RequestMapping("/api/superadmin/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Object>>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminVehicleService.getAllVehicles(pageable, search, status, type)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminVehicleService.getVehicleById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveVehicle(@PathVariable Long id) {
        adminVehicleService.approveVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectVehicle(@PathVariable Long id) {
        adminVehicleService.rejectVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle rejected successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<String>> suspendVehicle(@PathVariable Long id) {
        adminVehicleService.suspendVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle suspended successfully"));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<String>> reactivateVehicle(@PathVariable Long id) {
        adminVehicleService.reactivateVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle reactivated successfully"));
    }

    @PostMapping("/{id}/mark-sold")
    public ResponseEntity<ApiResponse<String>> markVehicleAsSold(@PathVariable Long id) {
        adminVehicleService.markVehicleAsSold(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle marked as sold successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVehicle(@PathVariable Long id) {
        adminVehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<Object>>> getPendingVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminVehicleService.getPendingVehicles(pageable)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<Page<Object>>> getFeaturedVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminVehicleService.getFeaturedVehicles(pageable)));
    }
}
