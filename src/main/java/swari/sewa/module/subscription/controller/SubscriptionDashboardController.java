package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.entity.ShopOwnerRemark;
import swari.sewa.module.subscription.repository.ShopOwnerRemarkRepository;
import swari.sewa.module.subscription.service.SubscriptionDashboardService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/superadmin/subscription/dashboard")
@RequiredArgsConstructor
public class SubscriptionDashboardController {

    private final SubscriptionDashboardService dashboardService;
    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopOwnerRemarkRepository remarkRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "30d") String timeRange) {
        SubscriptionDashboardResponse dashboard = dashboardService.getDashboard(timeRange);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard data retrieved successfully"));
    }

    @GetMapping("/unsubscribed")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getUnsubscribedShopOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ShopOwner> owners = shopOwnerRepository.findUnsubscribed(pageable);
        Page<Map<String, Object>> result = owners.map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            String shopName = o.getShopName();
            if (shopName == null || shopName.trim().isEmpty()) shopName = o.getCompanyName();
            m.put("shopName", shopName);
            m.put("ownerName", (o.getFirstName() != null ? o.getFirstName() : "") + " " + (o.getLastName() != null ? o.getLastName() : ""));
            m.put("email", o.getEmail());
            m.put("phone", o.getPhone());
            String address = o.getAddress();
            if (address == null || address.trim().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (o.getTole() != null && !o.getTole().trim().isEmpty()) sb.append(o.getTole()).append(", ");
                if (o.getMunicipality() != null && !o.getMunicipality().trim().isEmpty()) sb.append(o.getMunicipality()).append(", ");
                if (o.getWard() != null && !o.getWard().trim().isEmpty()) sb.append("Ward ").append(o.getWard()).append(", ");
                if (o.getDistrict() != null && !o.getDistrict().trim().isEmpty()) sb.append(o.getDistrict()).append(", ");
                if (o.getProvince() != null && !o.getProvince().trim().isEmpty()) sb.append(o.getProvince());
                address = sb.length() > 0 ? sb.toString().replaceAll(", $", "") : null;
            }
            m.put("address", address);
            m.put("createdAt", o.getCreatedAt());
            m.put("kycVerified", o.getKycVerified());
            // Attach latest remark
            remarkRepository.findFirstByShopOwnerIdOrderByCreatedAtDesc(o.getId())
                    .ifPresent(r -> {
                        m.put("latestRemark", r.getRemark());
                        m.put("latestRemarkDate", r.getCreatedAt());
                    });
            return m;
        });
        return ResponseEntity.ok(ApiResponse.success(result, "Unsubscribed shop owners retrieved successfully"));
    }

    @GetMapping("/remarks/{shopOwnerId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRemarks(@PathVariable Long shopOwnerId) {
        List<ShopOwnerRemark> remarks = remarkRepository.findByShopOwnerIdOrderByCreatedAtDesc(shopOwnerId);
        List<Map<String, Object>> result = remarks.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("remark", r.getRemark());
            m.put("createdAt", r.getCreatedAt());
            if (r.getAdminUserId() != null) {
                userRepository.findById(r.getAdminUserId()).ifPresent(u -> {
                    String name = (u.getFirstName() != null ? u.getFirstName() : "") + (u.getLastName() != null ? " " + u.getLastName() : "");
                    m.put("adminName", name.trim().isEmpty() ? u.getEmail() : name.trim());
                });
            }
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result, "Remarks retrieved successfully"));
    }

    @PostMapping("/remarks/{shopOwnerId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addRemark(@PathVariable Long shopOwnerId,
                                                                       @RequestBody Map<String, String> body) {
        String remark = body.get("remark");
        if (remark == null || remark.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Remark cannot be empty"));
        }
        Long adminId = getCurrentAdminUserId();
        ShopOwnerRemark saved = remarkRepository.save(ShopOwnerRemark.builder()
                .shopOwnerId(shopOwnerId)
                .remark(remark.trim())
                .adminUserId(adminId)
                .build());
        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("remark", saved.getRemark());
        result.put("createdAt", saved.getCreatedAt());
        if (adminId != null) {
            userRepository.findById(adminId).ifPresent(u -> {
                String name = (u.getFirstName() != null ? u.getFirstName() : "") + (u.getLastName() != null ? " " + u.getLastName() : "");
                result.put("adminName", name.trim().isEmpty() ? u.getEmail() : name.trim());
            });
        }
        return ResponseEntity.ok(ApiResponse.success(result, "Remark added successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
