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
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin/subscription/plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<SubscriptionPlanResponse>>> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "sortOrder,asc") String sort) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        Page<SubscriptionPlanResponse> plans = planService.getPlans(search, status, visibility, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(plans, "Plans retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(@Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = planService.createPlan(request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanById(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.getPlanById(id);
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(@PathVariable Long id, @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = planService.updatePlan(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Plan deleted successfully"));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> publishPlan(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.publishPlan(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan published successfully"));
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> archivePlan(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.archivePlan(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan archived successfully"));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> duplicatePlan(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.duplicatePlan(id, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(plan, "Plan duplicated successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
