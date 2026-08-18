package swari.sewa.module.shopowner.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.service.AdminShopOwnerService;

import java.util.Map;

/**
 * Self-service endpoints for shop owners (e.g. forced password change on first
 * login after admin approval, or voluntary password change from the profile
 * page).
 */
@RestController
@RequestMapping("/api/shopowner")
@RequiredArgsConstructor
public class ShopOwnerSelfServiceController {

    private final ShopOwnerRepository shopOwnerRepository;
    private final AdminShopOwnerService adminShopOwnerService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        ShopOwner owner = shopOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }

        adminShopOwnerService.changeShopOwnerPassword(owner.getId(), newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Voluntary password change from the profile page. Verifies the current
     * password before accepting the new one.
     */
    @PostMapping("/change-password-secure")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<String>> changePasswordSecure(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        ShopOwner owner = shopOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new RuntimeException("Current password is required.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters.");
        }
        if (!passwordEncoder.matches(currentPassword, owner.getPassword())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        adminShopOwnerService.changeShopOwnerPassword(owner.getId(), newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
