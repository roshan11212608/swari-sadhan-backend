package swari.sewa.module.shop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.shop.dto.ShopDto;
import swari.sewa.module.shop.dto.ShopReviewDto;
import swari.sewa.module.shop.dto.ShopReviewSummaryDto;
import swari.sewa.module.shop.dto.ShopReviewPageResponseDto;
import swari.sewa.module.shop.entity.ShopReview;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.shop.service.ShopService;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShopController {

    private final ShopService shopService;
    private final ShopReviewRepository shopReviewRepository;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ShopDto> createShop(@Valid @RequestBody ShopDto shopDto,
                                             @RequestHeader("X-User-Id") Long userId) {
        ShopDto createdShop = shopService.createShop(shopDto, userId);
        return ResponseEntity.ok(createdShop);
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<List<ShopReviewDto>> getMyShopReviews(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopIdHeader,
            org.springframework.security.core.Authentication authentication) {

        Long shopId = null;

        // 1. Try X-Shop-Id header (sent from frontend if available)
        if (shopIdHeader != null) {
            shopId = shopIdHeader;
        }

        // 2. Try looking up by user ID
        if (shopId == null && userId != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByUserId(userId);
            if (shopOpt.isPresent()) {
                shopId = shopOpt.get().getId();
            }
        }

        // 3. Fallback: look up by email from authentication
        if (shopId == null && authentication != null && authentication.getName() != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByEmail(authentication.getName());
            if (shopOpt.isPresent()) {
                shopId = shopOpt.get().getId();
            }
        }

        if (shopId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<ShopReviewDto> reviews = shopReviewRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopDto> getShopById(@PathVariable Long id) {
        Optional<ShopDto> shop = shopService.getShopById(id);
        return shop.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @userSecurity.isOwner(#userId, authentication.name))")
    public ResponseEntity<ShopDto> getShopByUserId(@PathVariable Long userId) {
        Optional<ShopDto> shop = shopService.getShopByUserId(userId);
        return shop.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ShopDto>> getAllShops() {
        List<ShopDto> shops = shopService.getAllShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<ShopDto>> getShopsByCity(@PathVariable String city) {
        List<ShopDto> shops = shopService.getShopsByCity(city);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/state/{state}")
    public ResponseEntity<List<ShopDto>> getShopsByState(@PathVariable String state) {
        List<ShopDto> shops = shopService.getShopsByState(state);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ShopDto>> getFeaturedShops() {
        List<ShopDto> shops = shopService.getFeaturedShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ShopDto>> searchShops(@RequestParam String keyword) {
        List<ShopDto> shops = shopService.searchShops(keyword);
        return ResponseEntity.ok(shops);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#id, authentication.name))")
    public ResponseEntity<ShopDto> updateShop(@PathVariable Long id, @Valid @RequestBody ShopDto shopDto) {
        ShopDto updatedShop = shopService.updateShop(id, shopDto);
        return ResponseEntity.ok(updatedShop);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        shopService.deleteShop(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ShopDto> approveShop(@PathVariable Long id) {
        ShopDto shop = shopService.approveShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ShopDto> rejectShop(@PathVariable Long id) {
        ShopDto shop = shopService.rejectShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ShopDto> activateShop(@PathVariable Long id) {
        ShopDto shop = shopService.activateShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ShopDto> deactivateShop(@PathVariable Long id) {
        ShopDto shop = shopService.deactivateShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ShopDto> suspendShop(@PathVariable Long id) {
        ShopDto shop = shopService.suspendShop(id);
        return ResponseEntity.ok(shop);
    }

    /* ===================== SHOP REVIEWS ===================== */

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ShopReviewDto>> getShopReviews(@PathVariable Long id) {
        List<ShopReviewDto> reviews = shopReviewRepository.findByShopIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('PUBLIC')")
    public ResponseEntity<ShopReviewDto> addShopReview(
            @PathVariable Long id,
            @RequestBody ShopReviewDto reviewDto,
            @RequestHeader("X-User-Id") Long userId) {

        if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }

        // Derive reviewer name from the authenticated user (not from the request body)
        User reviewer = userRepository.findById(userId).orElse(null);
        if (reviewer == null) {
            return ResponseEntity.badRequest().build();
        }
        String reviewerName = (reviewer.getFirstName() + " " + reviewer.getLastName()).trim();

        ShopReview review = ShopReview.builder()
                .shopId(id)
                .userId(userId)
                .reviewerName(reviewerName)
                .rating(reviewDto.getRating())
                .comment(reviewDto.getComment())
                .build();

        review = shopReviewRepository.save(review);
        return ResponseEntity.ok(toReviewDto(review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopIdHeader,
            org.springframework.security.core.Authentication authentication) {

        ShopReview review = shopReviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify the review belongs to the shop owner's shop
        Long ownerShopId = shopIdHeader;
        if (ownerShopId == null && userId != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByUserId(userId);
            if (shopOpt.isPresent()) {
                ownerShopId = shopOpt.get().getId();
            }
        }
        if (ownerShopId == null && authentication != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByEmail(authentication.getName());
            if (shopOpt.isPresent()) {
                ownerShopId = shopOpt.get().getId();
            }
        }

        if (ownerShopId == null || !ownerShopId.equals(review.getShopId())) {
            return ResponseEntity.status(403).build();
        }

        shopReviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    // ── Paginated + filtered review endpoints ──
    // Replaces loading ALL reviews + client-side filtering with server-side pagination.

    @GetMapping("/{id}/reviews/paged")
    public ResponseEntity<ShopReviewPageResponseDto> getShopReviewsPaged(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(buildPagedReviews(id, rating, search, page, size));
    }

    @GetMapping("/my-reviews/paged")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ShopReviewPageResponseDto> getMyShopReviewsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopIdHeader,
            org.springframework.security.core.Authentication authentication) {

        Long shopId = resolveShopId(userId, shopIdHeader, authentication);
        if (shopId == null) {
            return ResponseEntity.ok(ShopReviewPageResponseDto.builder()
                    .reviews(List.of())
                    .summary(buildSummary(null))
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .build());
        }

        return ResponseEntity.ok(buildPagedReviews(shopId, rating, search, page, size));
    }

    private Long resolveShopId(Long userId, Long shopIdHeader, org.springframework.security.core.Authentication authentication) {
        if (shopIdHeader != null) return shopIdHeader;
        if (userId != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByUserId(userId);
            if (shopOpt.isPresent()) return shopOpt.get().getId();
        }
        if (authentication != null && authentication.getName() != null) {
            Optional<ShopDto> shopOpt = shopService.getShopByEmail(authentication.getName());
            if (shopOpt.isPresent()) return shopOpt.get().getId();
        }
        return null;
    }

    private ShopReviewPageResponseDto buildPagedReviews(Long shopId, Integer rating, String search, int page, int size) {
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<ShopReview> reviewPage = shopReviewRepository.findByShopIdWithFilters(shopId, rating, normalizedSearch, pageable);

        List<ShopReviewDto> reviewDtos = reviewPage.getContent().stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());

        return ShopReviewPageResponseDto.builder()
                .reviews(reviewDtos)
                .summary(buildSummary(shopId))
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .build();
    }

    private ShopReviewSummaryDto buildSummary(Long shopId) {
        if (shopId == null) {
            return ShopReviewSummaryDto.builder()
                    .count(0)
                    .averageRating(0.0)
                    .distribution(List.of())
                    .build();
        }

        long count = shopReviewRepository.countByShopId(shopId);
        double avg = shopReviewRepository.getAverageRatingByShopId(shopId);

        // Build rating distribution from a single group-by query
        List<Object[]> ratingCounts = shopReviewRepository.countByShopIdGroupByRating(shopId);
        List<ShopReviewSummaryDto.RatingBreakdown> distribution = new ArrayList<>();
        // Initialize all stars 5..1 with 0, then fill from query results
        int[] countsByStar = new int[6]; // index 1..5
        for (Object[] row : ratingCounts) {
            int star = ((Number) row[0]).intValue();
            long cnt = ((Number) row[1]).longValue();
            if (star >= 1 && star <= 5) {
                countsByStar[star] = (int) cnt;
            }
        }
        for (int star = 5; star >= 1; star--) {
            distribution.add(ShopReviewSummaryDto.RatingBreakdown.builder()
                    .star(star)
                    .count(countsByStar[star])
                    .build());
        }

        return ShopReviewSummaryDto.builder()
                .count(count)
                .averageRating(avg)
                .distribution(distribution)
                .build();
    }

    private ShopReviewDto toReviewDto(ShopReview review) {
        return ShopReviewDto.builder()
                .id(review.getId())
                .shopId(review.getShopId())
                .userId(review.getUserId())
                .reviewerName(review.getReviewerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
