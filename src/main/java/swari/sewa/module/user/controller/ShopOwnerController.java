package swari.sewa.module.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.common.exception.StorageException;
import swari.sewa.common.service.ImageType;
import swari.sewa.common.service.StorageCategory;
import swari.sewa.common.service.StorageService;
import swari.sewa.module.user.dto.ShopOwnerProfileDto;
import swari.sewa.module.user.dto.KycSubmissionDto;
import swari.sewa.module.user.dto.SubscriptionPlanDto;
import swari.sewa.module.user.service.ShopOwnerProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shopowner/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SHOP_OWNER')")
public class ShopOwnerController {

    private final ShopOwnerProfileService shopOwnerProfileService;
    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopOwnerProfileDto>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getProfile()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopOwnerProfileDto>> updateProfile(
            @Valid @RequestBody ShopOwnerProfileDto profileDto) {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.updateProfile(profileDto)));
    }

    @PostMapping("/kyc")
    public ResponseEntity<ApiResponse<String>> submitKyc(
            @Valid @RequestBody KycSubmissionDto kycDto) {
        shopOwnerProfileService.submitKyc(kycDto);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted successfully"));
    }

    @GetMapping("/kyc/status")
    public ResponseEntity<ApiResponse<Object>> getKycStatus() {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getKycStatus()));
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> getSubscriptionPlan() {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getSubscriptionPlan()));
    }

    @PostMapping("/subscription/upgrade")
    public ResponseEntity<ApiResponse<String>> upgradeSubscription(
            @RequestBody String plan) {
        shopOwnerProfileService.upgradeSubscription(plan);
        return ResponseEntity.ok(ApiResponse.success("Subscription upgraded successfully"));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getDashboardStats()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Object>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getBookings(page, size)));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Object>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(shopOwnerProfileService.getCustomers(page, size)));
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "PROFILE_PHOTO") String uploadType) {
        ProfileUploadType resolved = ProfileUploadType.fromString(uploadType);
        ShopOwnerProfileDto current = shopOwnerProfileService.getProfile();

        Long entityId = resolved.getEntityId(current);
        StorageCategory category = resolved.getCategory();
        ImageType imageType = resolved.getImageType();

        String fileUrl = storageService.store(file, category, entityId, imageType);

        if (resolved == ProfileUploadType.PROFILE_PHOTO) {
            shopOwnerProfileService.updateProfilePhoto(fileUrl);
            String oldUrl = current != null ? current.getProfilePhoto() : null;
            if (oldUrl != null && !oldUrl.isBlank() && !oldUrl.equals(fileUrl)) {
                storageService.deleteByUrl(oldUrl);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(fileUrl));
    }

    private enum ProfileUploadType {
        PROFILE_PHOTO(StorageCategory.USER, ImageType.PROFILE_PHOTO) {
            @Override
            Long getEntityId(ShopOwnerProfileDto current) {
                return current != null ? current.getId() : null;
            }
        },
        SHOP_LOGO(StorageCategory.SHOP, ImageType.SHOP_LOGO) {
            @Override
            Long getEntityId(ShopOwnerProfileDto current) {
                return current != null ? current.getShopId() : null;
            }
        },
        CITIZENSHIP_PIC_FRONT(StorageCategory.USER, ImageType.USER_DOCUMENT) {
            @Override
            Long getEntityId(ShopOwnerProfileDto current) {
                return current != null ? current.getId() : null;
            }
        },
        CITIZENSHIP_PIC_BACK(StorageCategory.USER, ImageType.USER_DOCUMENT) {
            @Override
            Long getEntityId(ShopOwnerProfileDto current) {
                return current != null ? current.getId() : null;
            }
        },
        SHOP_REG_UPLOAD(StorageCategory.SHOP_REGISTRATION, ImageType.SHOP_REGISTRATION_DOC) {
            @Override
            Long getEntityId(ShopOwnerProfileDto current) {
                return current != null ? current.getId() : null;
            }
        };

        private final StorageCategory category;
        private final ImageType imageType;

        ProfileUploadType(StorageCategory category, ImageType imageType) {
            this.category = category;
            this.imageType = imageType;
        }

        StorageCategory getCategory() { return category; }
        ImageType getImageType() { return imageType; }
        abstract Long getEntityId(ShopOwnerProfileDto current);

        static ProfileUploadType fromString(String value) {
            if (value == null || value.isBlank()) {
                return PROFILE_PHOTO;
            }
            try {
                return valueOf(value.toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException e) {
                throw new StorageException("Unsupported upload type: " + value);
            }
        }
    }
}
