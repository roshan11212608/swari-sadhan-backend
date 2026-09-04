package swari.sewa.module.shop.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import swari.sewa.module.shop.dto.ShopReviewPageResponseDto;
import swari.sewa.module.shop.entity.ShopReview;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.shop.service.ShopService;
import swari.sewa.module.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression tests for Shop Reviews pagination + summary optimization.
 *
 * <p>Verifies that the paged review endpoints:
 * <ul>
 *   <li>Use paginated queries instead of loading ALL reviews.</li>
 *   <li>Use count + avg + group-by queries for summary stats.</li>
 *   <li>Apply server-side rating and search filters.</li>
 *   <li>Do NOT call findByShopIdOrderByCreatedAtDesc (the unpaginated method).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ShopReviewPaginationTest {

    @Mock private ShopService shopService;
    @Mock private ShopReviewRepository shopReviewRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ShopController shopController;

    private ShopReview createTestReview(Long id, int rating, String reviewerName, String comment) {
        return ShopReview.builder()
                .id(id)
                .shopId(1L)
                .userId(10L + id)
                .reviewerName(reviewerName)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now().minusDays(id))
                .build();
    }

    @Test
    void getShopReviewsPaged_usesPaginatedQuery_notFindAll() {
        Long shopId = 1L;
        List<ShopReview> reviews = List.of(
                createTestReview(1L, 5, "Alice", "Great service"),
                createTestReview(2L, 4, "Bob", "Good experience")
        );

        when(shopReviewRepository.findByShopIdWithFilters(eq(shopId), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reviews, PageRequest.of(0, 10), 2));
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(2L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(4.5);
        when(shopReviewRepository.countByShopIdGroupByRating(shopId))
                .thenReturn(List.of(new Object[]{5, 1L}, new Object[]{4, 1L}));

        ShopReviewPageResponseDto result = shopController.getShopReviewsPaged(
                shopId, 0, 10, null, null).getBody();

        assertNotNull(result);
        assertEquals(2, result.getReviews().size());
        assertEquals(2L, result.getSummary().getCount());
        assertEquals(4.5, result.getSummary().getAverageRating(), 0.01);
        assertEquals(5, result.getSummary().getDistribution().size()); // stars 5..1

        // Verify distribution order (5 → 1)
        assertEquals(5, result.getSummary().getDistribution().get(0).getStar());
        assertEquals(1, result.getSummary().getDistribution().get(0).getCount());
        assertEquals(4, result.getSummary().getDistribution().get(1).getStar());
        assertEquals(1, result.getSummary().getDistribution().get(1).getCount());

        // Verify paginated query was used
        verify(shopReviewRepository).findByShopIdWithFilters(eq(shopId), eq(null), eq(null), any(PageRequest.class));
        verify(shopReviewRepository).countByShopId(shopId);
        verify(shopReviewRepository).getAverageRatingByShopId(shopId);
        verify(shopReviewRepository).countByShopIdGroupByRating(shopId);

        // Verify unpaginated method was NOT called
        verify(shopReviewRepository, never()).findByShopIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void getShopReviewsPaged_withRatingFilter_appliesServerSide() {
        Long shopId = 1L;
        List<ShopReview> fiveStarReviews = List.of(
                createTestReview(1L, 5, "Alice", "Amazing!")
        );

        when(shopReviewRepository.findByShopIdWithFilters(eq(shopId), eq(5), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(fiveStarReviews, PageRequest.of(0, 10), 1));
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(10L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(4.2);
        when(shopReviewRepository.countByShopIdGroupByRating(shopId))
                .thenReturn(List.of(new Object[]{5, 5L}, new Object[]{4, 3L}, new Object[]{3, 2L}));

        ShopReviewPageResponseDto result = shopController.getShopReviewsPaged(
                shopId, 0, 10, 5, null).getBody();

        assertNotNull(result);
        assertEquals(1, result.getReviews().size());
        assertEquals(5, result.getReviews().get(0).getRating());

        // Summary reflects ALL reviews (not just filtered)
        assertEquals(10L, result.getSummary().getCount());

        // Verify rating filter was passed to repository
        verify(shopReviewRepository).findByShopIdWithFilters(eq(shopId), eq(5), eq(null), any(PageRequest.class));
    }

    @Test
    void getShopReviewsPaged_withSearchFilter_appliesServerSide() {
        Long shopId = 1L;
        List<ShopReview> searchResults = List.of(
                createTestReview(1L, 5, "Alice", "Great service")
        );

        when(shopReviewRepository.findByShopIdWithFilters(eq(shopId), eq(null), eq("alice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(searchResults, PageRequest.of(0, 10), 1));
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(50L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(3.8);
        when(shopReviewRepository.countByShopIdGroupByRating(shopId))
                .thenReturn(List.of(new Object[]{5, 20L}, new Object[]{4, 15L}, new Object[]{3, 10L}, new Object[]{2, 3L}, new Object[]{1, 2L}));

        ShopReviewPageResponseDto result = shopController.getShopReviewsPaged(
                shopId, 0, 10, null, "alice").getBody();

        assertNotNull(result);
        assertEquals(1, result.getReviews().size());
        assertEquals("Alice", result.getReviews().get(0).getReviewerName());

        // Summary reflects ALL reviews (not just search results)
        assertEquals(50L, result.getSummary().getCount());

        verify(shopReviewRepository).findByShopIdWithFilters(eq(shopId), eq(null), eq("alice"), any(PageRequest.class));
    }

    @Test
    void getShopReviewsPaged_withNoData_returnsEmptyResponse() {
        Long shopId = 999L;

        when(shopReviewRepository.findByShopIdWithFilters(eq(shopId), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(0L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(0.0);
        when(shopReviewRepository.countByShopIdGroupByRating(shopId))
                .thenReturn(List.of());

        ShopReviewPageResponseDto result = shopController.getShopReviewsPaged(
                shopId, 0, 10, null, null).getBody();

        assertNotNull(result);
        assertEquals(0, result.getReviews().size());
        assertEquals(0L, result.getSummary().getCount());
        assertEquals(0.0, result.getSummary().getAverageRating(), 0.01);
        // Distribution should still have 5 entries (all with count 0)
        assertEquals(5, result.getSummary().getDistribution().size());
        assertEquals(0, result.getSummary().getDistribution().get(0).getCount());
    }

    @Test
    void getShopReviewsPaged_distributionCoversAllStars() {
        Long shopId = 1L;

        when(shopReviewRepository.findByShopIdWithFilters(eq(shopId), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(100L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(4.1);
        // Only 5-star and 1-star reviews exist
        when(shopReviewRepository.countByShopIdGroupByRating(shopId))
                .thenReturn(List.of(new Object[]{5, 80L}, new Object[]{1, 20L}));

        ShopReviewPageResponseDto result = shopController.getShopReviewsPaged(
                shopId, 0, 10, null, null).getBody();

        // Distribution should have 5 entries (5,4,3,2,1) even if some have 0 count
        assertEquals(5, result.getSummary().getDistribution().size());
        assertEquals(5, result.getSummary().getDistribution().get(0).getStar());
        assertEquals(80, result.getSummary().getDistribution().get(0).getCount());
        assertEquals(4, result.getSummary().getDistribution().get(1).getStar());
        assertEquals(0, result.getSummary().getDistribution().get(1).getCount());
        assertEquals(3, result.getSummary().getDistribution().get(2).getStar());
        assertEquals(0, result.getSummary().getDistribution().get(2).getCount());
        assertEquals(2, result.getSummary().getDistribution().get(3).getStar());
        assertEquals(0, result.getSummary().getDistribution().get(3).getCount());
        assertEquals(1, result.getSummary().getDistribution().get(4).getStar());
        assertEquals(20, result.getSummary().getDistribution().get(4).getCount());
    }
}
