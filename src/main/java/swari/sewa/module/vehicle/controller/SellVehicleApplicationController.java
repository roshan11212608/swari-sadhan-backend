package swari.sewa.module.vehicle.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.service.SellVehicleApplicationService;

@RestController
@RequestMapping("/api/sell-applications")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        maxAge = 3600
)
public class SellVehicleApplicationController {

    private final SellVehicleApplicationService applicationService;
    private final ShopRepository shopRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    /* ===================== HELPERS ===================== */

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (a != null && target.equals(a.getAuthority())) return true;
        }
        return false;
    }

    private Long resolveCurrentShopIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;

        String email = authentication.getName();
        ShopOwner shopOwner = shopOwnerRepository.findByEmail(email).orElse(null);
        if (shopOwner == null) return null;

        return shopRepository.findByShopOwnerId(shopOwner.getId())
                .stream()
                .findFirst()
                .map(Shop::getId)
                .orElse(null);
    }

    /* ===================== CREATE APPLICATION (WITH PHOTOS) ===================== */

    @PostMapping(
            value = "/vehicle/{vehicleId}",
            consumes = "multipart/form-data"
    )
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> createApplication(
            @PathVariable Long vehicleId,

            // JSON data
            @RequestPart("data") SellVehicleApplicationDto applicationDto,

            // Photos
            @RequestPart(value = "customerPhoto", required = false) MultipartFile customerPhoto,
            @RequestPart(value = "citizenshipFrontPhoto", required = false) MultipartFile citizenshipFrontPhoto,
            @RequestPart(value = "citizenshipBackPhoto", required = false) MultipartFile citizenshipBackPhoto
    ) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long shopId;

            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                shopId = resolveCurrentShopIdOrNull();
                if (shopId == null) {
                    return ResponseEntity.status(404).body(Map.of(
                            "error", "Shop not found",
                            "message", "No shop found for this account"
                    ));
                }
            } else {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Access denied",
                        "message", "Only shop owners can create applications"
                ));
            }

            /* ===== SAVE FILES (TEMP SIMPLE LOGIC) =====
               Replace this with your real file-storage service
            */
            if (customerPhoto != null && !customerPhoto.isEmpty()) {
                applicationDto.setCustomerPhoto(customerPhoto.getOriginalFilename());
            }
            if (citizenshipFrontPhoto != null && !citizenshipFrontPhoto.isEmpty()) {
                applicationDto.setCitizenshipFrontPhoto(citizenshipFrontPhoto.getOriginalFilename());
            }
            if (citizenshipBackPhoto != null && !citizenshipBackPhoto.isEmpty()) {
                applicationDto.setCitizenshipBackPhoto(citizenshipBackPhoto.getOriginalFilename());
            }

            SellVehicleApplicationDto created =
                    applicationService.createApplication(applicationDto, vehicleId, shopId);

            return ResponseEntity.ok(created);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create application",
                    "message", e.getMessage()
            ));
        }
    }

    /* ===================== OTHER APIS (UNCHANGED) ===================== */

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SellVehicleApplicationDto> getApplicationById(@PathVariable Long id) {
        Long shopId = resolveCurrentShopIdOrNull();
        if (shopId == null) return ResponseEntity.notFound().build();

        return applicationService.getApplicationById(id, shopId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Page<SellVehicleApplicationDto>> getApplicationsByShop(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long shopId = resolveCurrentShopIdOrNull();
        if (shopId == null) return ResponseEntity.notFound().build();

        Page<SellVehicleApplicationDto> applications =
                applicationService.getApplicationsByShop(
                        shopId,
                        org.springframework.data.domain.PageRequest.of(page, size)
                );

        return ResponseEntity.ok(applications);
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<java.util.List<SellVehicleApplicationDto>> getApplicationsByVehicle(@PathVariable Long vehicleId) {
        Long shopId = resolveCurrentShopIdOrNull();
        if (shopId == null) return ResponseEntity.notFound().build();

        java.util.List<SellVehicleApplicationDto> applications = applicationService.getApplicationsByVehicleAndShop(vehicleId, shopId);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Long shopId = resolveCurrentShopIdOrNull();
        if (shopId == null) return ResponseEntity.notFound().build();

        ApplicationStatus status =
                ApplicationStatus.valueOf(request.get("status").toString().toUpperCase());

        String notes = (String) request.get("notes");

        SellVehicleApplicationDto updated =
                applicationService.updateApplicationStatus(id, status, shopId, notes);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        Long shopId = resolveCurrentShopIdOrNull();
        if (shopId == null) return ResponseEntity.notFound().build();

        applicationService.deleteApplication(id, shopId);
        return ResponseEntity.noContent().build();
    }
}
