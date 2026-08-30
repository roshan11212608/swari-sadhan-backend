package swari.sewa.module.banner.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.banner.dto.BannerDto;
import swari.sewa.module.banner.service.BannerService;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerDto>>> getActiveBanners() {
        List<BannerDto> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(ApiResponse.success(banners));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<BannerDto>>> getAllBanners() {
        List<BannerDto> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(ApiResponse.success(banners));
    }

    @GetMapping("/position/{position}")
    public ResponseEntity<ApiResponse<BannerDto>> getBannerByPosition(@PathVariable String position) {
        BannerDto banner = bannerService.getBannerByPosition(position);
        return ResponseEntity.ok(ApiResponse.success(banner));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<BannerDto>> createBanner(@RequestBody BannerDto bannerDto) {
        BannerDto created = bannerService.createBanner(bannerDto);
        return ResponseEntity.ok(ApiResponse.success(created, "Banner created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<BannerDto>> updateBanner(@PathVariable Long id, @RequestBody BannerDto bannerDto) {
        BannerDto updated = bannerService.updateBanner(id, bannerDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Banner updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Banner deleted successfully"));
    }
}
