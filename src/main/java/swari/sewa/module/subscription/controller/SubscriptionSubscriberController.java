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
import swari.sewa.module.subscription.service.SubscriptionService;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/superadmin/subscription/subscribers")
@RequiredArgsConstructor
public class SubscriptionSubscriberController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<SubscriberResponse>>> getSubscribers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        Page<SubscriberResponse> subscribers = subscriptionService.getSubscribers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(subscribers, "Subscribers retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriberDetailsResponse>> getSubscriberById(@PathVariable Long id) {
        SubscriberDetailsResponse subscriber = subscriptionService.getSubscriberById(id);
        return ResponseEntity.ok(ApiResponse.success(subscriber, "Subscriber retrieved successfully"));
    }

    @PutMapping("/{id}/upgrade")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriberResponse>> upgradeSubscription(@PathVariable Long id,
                                                                              @Valid @RequestBody UpgradeSubscriptionRequest request) {
        SubscriberResponse subscriber = subscriptionService.upgradeSubscription(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(subscriber, "Subscription upgraded successfully"));
    }

    @PutMapping("/{id}/downgrade")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriberResponse>> downgradeSubscription(@PathVariable Long id,
                                                                                @Valid @RequestBody DowngradeSubscriptionRequest request) {
        SubscriberResponse subscriber = subscriptionService.downgradeSubscription(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(subscriber, "Subscription downgraded successfully"));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriberResponse>> suspendSubscription(@PathVariable Long id,
                                                                              @Valid @RequestBody SuspendSubscriptionRequest request) {
        SubscriberResponse subscriber = subscriptionService.suspendSubscription(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(subscriber, "Subscription suspended successfully"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SubscriberResponse>> cancelSubscription(@PathVariable Long id,
                                                                             @Valid @RequestBody CancelSubscriptionRequest request) {
        SubscriberResponse subscriber = subscriptionService.cancelSubscription(id, request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(subscriber, "Subscription cancelled successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
