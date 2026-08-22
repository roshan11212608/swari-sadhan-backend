package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.service.SubscriptionTrialService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/superadmin/subscription/trial")
@RequiredArgsConstructor
public class SubscriptionTrialController {

    private final SubscriptionTrialService trialService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<TrialResponse>> getTrial() {
        TrialResponse trial = trialService.getTrial();
        return ResponseEntity.ok(ApiResponse.success(trial, "Trial configuration retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<TrialResponse>> updateTrial(@Valid @RequestBody UpdateTrialRequest request) {
        TrialResponse trial = trialService.updateTrial(request, getCurrentAdminUserId());
        return ResponseEntity.ok(ApiResponse.success(trial, "Trial configuration updated successfully"));
    }

    /**
     * Returns the list of shop owners currently on a trial subscription.
     */
    @GetMapping("/subscribers")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<TrialSubscriberDto>>> getTrialSubscribers() {
        List<Subscription> trials = subscriptionRepository.findAllTrials();
        List<TrialSubscriberDto> subscribers = new ArrayList<>();
        for (Subscription sub : trials) {
            ShopOwner owner = shopOwnerRepository.findById(sub.getShopOwnerId()).orElse(null);
            if (owner == null) continue;

            String planName = sub.getPlan() != null ? sub.getPlan().getName() : null;
            int daysRemaining = sub.getEndDate() != null
                    ? (int) Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate()))
                    : 0;

            // Build shop address from available fields
            StringBuilder addressBuilder = new StringBuilder();
            if (owner.getAddress() != null && !owner.getAddress().isBlank()) addressBuilder.append(owner.getAddress());
            if (owner.getTole() != null && !owner.getTole().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getTole());
            }
            if (owner.getWard() != null && !owner.getWard().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append("Ward ").append(owner.getWard());
            }
            if (owner.getMunicipality() != null && !owner.getMunicipality().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getMunicipality());
            }
            if (owner.getDistrict() != null && !owner.getDistrict().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getDistrict());
            }
            String shopAddress = addressBuilder.length() > 0 ? addressBuilder.toString() : null;

            subscribers.add(TrialSubscriberDto.builder()
                    .subscriptionId(sub.getId())
                    .shopOwnerId(owner.getId())
                    .shopOwnerName((owner.getFirstName() != null ? owner.getFirstName() : "") +
                            (owner.getLastName() != null ? " " + owner.getLastName() : "").trim())
                    .email(owner.getEmail())
                    .phone(owner.getPhone())
                    .shopName(owner.getShopName())
                    .shopAddress(shopAddress)
                    .planName(planName)
                    .startDate(sub.getStartDate())
                    .endDate(sub.getEndDate())
                    .daysRemaining(daysRemaining)
                    .active(sub.getEndDate() != null && sub.getEndDate().isAfter(LocalDateTime.now()))
                    .build());
        }
        return ResponseEntity.ok(ApiResponse.success(subscribers, "Trial subscribers retrieved successfully"));
    }

    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    /**
     * Returns shop owners whose free trial expired within the last 7 days
     * and who have NOT purchased a paid subscription.
     */
    @GetMapping("/expired")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<ExpiredTrialDto>>> getExpiredTrialsWithoutSubscription() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Subscription> expiredTrials = subscriptionRepository.findRecentlyExpiredTrialsWithoutSubscription(since);
        List<ExpiredTrialDto> result = new ArrayList<>();
        for (Subscription sub : expiredTrials) {
            ShopOwner owner = shopOwnerRepository.findById(sub.getShopOwnerId()).orElse(null);
            if (owner == null) continue;

            String planName = sub.getPlanNameSnapshot() != null ? sub.getPlanNameSnapshot()
                    : (sub.getPlan() != null ? sub.getPlan().getName() : null);
            int daysSinceExpiry = sub.getEndDate() != null
                    ? (int) ChronoUnit.DAYS.between(sub.getEndDate(), LocalDateTime.now())
                    : 0;

            StringBuilder addressBuilder = new StringBuilder();
            if (owner.getAddress() != null && !owner.getAddress().isBlank()) addressBuilder.append(owner.getAddress());
            if (owner.getTole() != null && !owner.getTole().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getTole());
            }
            if (owner.getWard() != null && !owner.getWard().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append("Ward ").append(owner.getWard());
            }
            if (owner.getMunicipality() != null && !owner.getMunicipality().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getMunicipality());
            }
            if (owner.getDistrict() != null && !owner.getDistrict().isBlank()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(owner.getDistrict());
            }
            String shopAddress = addressBuilder.length() > 0 ? addressBuilder.toString() : null;

            result.add(ExpiredTrialDto.builder()
                    .subscriptionId(sub.getId())
                    .shopOwnerId(owner.getId())
                    .shopOwnerName((owner.getFirstName() != null ? owner.getFirstName() : "") +
                            (owner.getLastName() != null ? " " + owner.getLastName() : "").trim())
                    .email(owner.getEmail())
                    .phone(owner.getPhone())
                    .shopName(owner.getShopName())
                    .shopAddress(shopAddress)
                    .planName(planName)
                    .startDate(sub.getStartDate())
                    .endDate(sub.getEndDate())
                    .daysSinceExpiry(daysSinceExpiry)
                    .hasPaidSubscription(false)
                    .build());
        }
        return ResponseEntity.ok(ApiResponse.success(result, "Recently expired trials without subscription retrieved"));
    }
}
