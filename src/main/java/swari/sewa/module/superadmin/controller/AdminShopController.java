package swari.sewa.module.superadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.superadmin.service.AdminShopService;

@RestController
@RequestMapping("/api/superadmin/shops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminShopController {

    private final AdminShopService adminShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Object>>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminShopService.getAllShops(pageable, search, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getShopById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminShopService.getShopById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveShop(@PathVariable Long id) {
        adminShopService.approveShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectShop(@PathVariable Long id) {
        adminShopService.rejectShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop rejected successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<String>> suspendShop(@PathVariable Long id) {
        adminShopService.suspendShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop suspended successfully"));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<String>> reactivateShop(@PathVariable Long id) {
        adminShopService.reactivateShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop reactivated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShop(@PathVariable Long id) {
        adminShopService.deleteShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop deleted successfully"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<Object>>> getPendingShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminShopService.getPendingShops(pageable)));
    }
}
