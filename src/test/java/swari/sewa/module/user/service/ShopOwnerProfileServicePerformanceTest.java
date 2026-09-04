package swari.sewa.module.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.service.impl.ShopOwnerProfileServiceImpl;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for ShopOwnerProfileServiceImpl performance optimizations.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getDashboardStats uses a single aggregate rating query instead of an N+1 loop</li>
 *   <li>getDashboardStats no longer calls getAverageRatingByShopId or countByShopId per shop</li>
 *   <li>getDashboardStats correctly computes averageRating from the aggregate query result</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ShopOwnerProfileServicePerformanceTest {

    @Mock private ShopOwnerRepository shopOwnerRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private ShopReviewRepository shopReviewRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EnquiryRepository enquiryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ShopOwnerProfileServiceImpl profileService;

    private void mockAuthentication(Long shopOwnerId, String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        ShopOwner shopOwner = new ShopOwner();
        shopOwner.setId(shopOwnerId);
        shopOwner.setEmail(email);
        when(shopOwnerRepository.findByEmail(email)).thenReturn(Optional.of(shopOwner));
    }

    @Test
    void getDashboardStats_usesAggregateRatingQuery_notN1Loop() {
        mockAuthentication(1L, "owner@test.com");

        when(shopRepository.countByShopOwner_Id(1L)).thenReturn(2L);
        when(shopRepository.countByShopOwner_IdAndStatus(1L, ShopStatus.ACTIVE)).thenReturn(2L);
        when(vehicleRepository.countByShop_ShopOwner_Id(1L)).thenReturn(10L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.ACTIVE)).thenReturn(7L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.SOLD)).thenReturn(3L);
        when(enquiryRepository.countByShop_ShopOwner_Id(1L)).thenReturn(15L);
        when(enquiryRepository.countByShop_ShopOwner_IdAndStatus(1L, EnquiryStatus.PENDING)).thenReturn(5L);

        // Aggregate query returns [count, sum_of_ratings]
        // e.g., 10 reviews with total rating sum of 42 → avg = 4.2
        when(shopReviewRepository.aggregateRatingByShopOwnerId(1L))
                .thenReturn(new Object[]{10L, 42.0});

        Map<String, Object> stats = profileService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(2L, stats.get("totalShops"));
        assertEquals(10L, stats.get("totalVehicles"));
        assertEquals(7L, stats.get("activeVehicles"));
        assertEquals(3L, stats.get("soldVehicles"));
        assertEquals(15L, stats.get("totalEnquiries"));
        assertEquals(5L, stats.get("pendingEnquiries"));
        assertEquals(10L, stats.get("ratingCount"));
        // avg = 42.0 / 10 = 4.2, rounded to 1 decimal = 4.2
        assertEquals(4.2, stats.get("rating"));

        // Verify the aggregate query was used
        verify(shopReviewRepository).aggregateRatingByShopOwnerId(1L);

        // Verify the old per-shop methods are NOT called
        verify(shopReviewRepository, never()).getAverageRatingByShopId(anyLong());
        verify(shopReviewRepository, never()).countByShopId(anyLong());
        verify(shopRepository, never()).findByShopOwnerId(anyLong());
    }

    @Test
    void getDashboardStats_handlesNoReviews() {
        mockAuthentication(1L, "owner@test.com");

        when(shopRepository.countByShopOwner_Id(1L)).thenReturn(1L);
        when(shopRepository.countByShopOwner_IdAndStatus(1L, ShopStatus.ACTIVE)).thenReturn(1L);
        when(vehicleRepository.countByShop_ShopOwner_Id(1L)).thenReturn(5L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.ACTIVE)).thenReturn(4L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.SOLD)).thenReturn(1L);
        when(enquiryRepository.countByShop_ShopOwner_Id(1L)).thenReturn(3L);
        when(enquiryRepository.countByShop_ShopOwner_IdAndStatus(1L, EnquiryStatus.PENDING)).thenReturn(1L);

        // No reviews: count=0, sum=0
        when(shopReviewRepository.aggregateRatingByShopOwnerId(1L))
                .thenReturn(new Object[]{0L, 0.0});

        Map<String, Object> stats = profileService.getDashboardStats();

        assertEquals(0L, stats.get("ratingCount"));
        assertEquals(0.0, stats.get("rating"));
    }

    @Test
    void getDashboardStats_handlesNullAggregateResult() {
        mockAuthentication(1L, "owner@test.com");

        when(shopRepository.countByShopOwner_Id(1L)).thenReturn(1L);
        when(shopRepository.countByShopOwner_IdAndStatus(1L, ShopStatus.ACTIVE)).thenReturn(1L);
        when(vehicleRepository.countByShop_ShopOwner_Id(1L)).thenReturn(5L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.ACTIVE)).thenReturn(4L);
        when(vehicleRepository.countByShop_ShopOwner_IdAndStatus(1L, VehicleStatus.SOLD)).thenReturn(1L);
        when(enquiryRepository.countByShop_ShopOwner_Id(1L)).thenReturn(3L);
        when(enquiryRepository.countByShop_ShopOwner_IdAndStatus(1L, EnquiryStatus.PENDING)).thenReturn(1L);

        // Null result (should not happen in practice, but should be handled gracefully)
        when(shopReviewRepository.aggregateRatingByShopOwnerId(1L)).thenReturn(null);

        Map<String, Object> stats = profileService.getDashboardStats();

        assertEquals(0L, stats.get("ratingCount"));
        assertEquals(0.0, stats.get("rating"));
    }
}
