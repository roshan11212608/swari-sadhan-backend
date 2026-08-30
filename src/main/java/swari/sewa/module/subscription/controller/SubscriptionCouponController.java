package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.service.SubscriptionCouponService;

import java.util.List;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin/subscription/coupons")
@RequiredArgsConstructor
public class SubscriptionCouponController {

    private final SubscriptionCouponService couponService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        Page<CouponResponse> coupons = couponService.getCoupons(search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(coupons, "Coupons retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        CouponResponse coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        CouponResponse coupon = couponService.createCoupon(request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateCouponRequest request) {
        CouponResponse coupon = couponService.updateCoupon(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Coupon deleted successfully"));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCoupon(@PathVariable Long id) {
        CouponResponse coupon = couponService.toggleCoupon(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon toggled successfully"));
    }

    @GetMapping("/{id}/usages")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<CouponUsageResponse>>> getCouponUsages(@PathVariable Long id) {
        List<CouponUsageResponse> usages = couponService.getCouponUsages(id);
        return ResponseEntity.ok(ApiResponse.success(usages, "Coupon usages retrieved successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
