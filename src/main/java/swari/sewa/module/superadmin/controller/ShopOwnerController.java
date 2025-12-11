package swari.sewa.module.superadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;
import swari.sewa.module.superadmin.service.ShopOwnerService;

import jakarta.validation.Valid;

@RestController("superAdminShopOwnerController")
@RequestMapping("/api/superadmin/shop-owners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class ShopOwnerController {

    private final ShopOwnerService shopOwnerService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShopOwnerDto>>> getAllShopOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(shopOwnerService.getAllShopOwners(pageable, search, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopOwnerDto>> getShopOwnerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerService.getShopOwnerById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveShopOwner(@PathVariable Long id) {
        shopOwnerService.approveShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectShopOwner(@PathVariable Long id) {
        shopOwnerService.rejectShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner rejected successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<String>> suspendShopOwner(@PathVariable Long id) {
        shopOwnerService.suspendShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner suspended successfully"));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<String>> reactivateShopOwner(@PathVariable Long id) {
        shopOwnerService.reactivateShopOwner(id);
        return ResponseEntity.ok(ApiResponse.success("Shop owner reactivated successfully"));
    }

    @GetMapping("/{id}/shops")
    public ResponseEntity<ApiResponse<Page<Object>>> getShopOwnerShops(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(shopOwnerService.getShopOwnerShops(id, pageable)));
    }

    @GetMapping("/{id}/vehicles")
    public ResponseEntity<ApiResponse<Page<Object>>> getShopOwnerVehicles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(shopOwnerService.getShopOwnerVehicles(id, pageable)));
    }
}
