package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin/subscription/settings")
@RequiredArgsConstructor
public class SubscriptionSettingsController {

    private final SubscriptionSettingsService settingsService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionSettingsResponse>> getSettings() {
        SubscriptionSettingsResponse settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateSubscriptionSettingsRequest request) {
        SubscriptionSettingsResponse settings = settingsService.updateSettings(request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
