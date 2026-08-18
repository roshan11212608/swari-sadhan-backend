package swari.sewa.module.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.user.service.AdminShopOwnerService;

import jakarta.validation.Valid;

@RestController("superAdminShopOwnerController")
@RequestMapping("/api/superadmin/shop-owners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminShopOwnerController {

    private final AdminShopOwnerService adminShopOwnerService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShopOwnerDto>> createShopOwner(@Valid @RequestBody ShopOwnerDto shopOwnerDto) {
        ShopOwnerDto created = adminShopOwnerService.createShopOwner(shopOwnerDto);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopOwnerDto>> updateShopOwner(@PathVariable Long id, @Valid @RequestBody ShopOwnerDto shopOwnerDto) {
        ShopOwnerDto updated = adminShopOwnerService.updateShopOwner(id, shopOwnerDto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShopOwner(@PathVariable Long id) {
        adminShopOwnerService.deleteShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShopOwnerDto>>> getAllShopOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminShopOwnerService.getAllShopOwners(pageable, search, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopOwnerDto>> getShopOwnerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminShopOwnerService.getShopOwnerById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveShopOwner(@PathVariable Long id) {
        adminShopOwnerService.approveShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectShopOwner(@PathVariable Long id,
                                                                @RequestParam(required = false) String reason) {
        adminShopOwnerService.rejectShopOwner(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Shop owner rejected successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<String>> suspendShopOwner(@PathVariable Long id) {
        adminShopOwnerService.suspendShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner suspended successfully"));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<String>> reactivateShopOwner(@PathVariable Long id) {
        adminShopOwnerService.reactivateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner reactivated successfully"));
    }

    @GetMapping("/{id}/shops")
    public ResponseEntity<ApiResponse<Page<Object>>> getShopOwnerShops(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminShopOwnerService.getShopOwnerShops(id, pageable)));
    }

    @GetMapping("/{id}/vehicles")
    public ResponseEntity<ApiResponse<Page<Object>>> getShopOwnerVehicles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminShopOwnerService.getShopOwnerVehicles(id, pageable)));
    }
}
