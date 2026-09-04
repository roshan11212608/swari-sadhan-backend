package swari.sewa.module.dashboard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.dashboard.dto.ShopDashboardSummaryDto;
import swari.sewa.module.dashboard.service.impl.DashboardServiceImpl;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.shop.entity.ShopReview;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression tests for ShopOwner Dashboard summary optimization.
 *
 * <p>Verifies that getShopDashboardSummary:
 * <ul>
 *   <li>Uses count queries instead of loading full entity lists.</li>
 *   <li>Uses paginated queries for recent items (5 per type).</li>
 *   <li>Does NOT call findAll or load large lists.</li>
 *   <li>Returns correct counts and recent items.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ShopDashboardSummaryTest {

    @Mock private ShopOwnerRepository shopOwnerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private ShopReviewRepository shopReviewRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EnquiryRepository enquiryRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Vehicle createTestVehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setTitle("Test Vehicle " + id);
        vehicle.setBrandName("Honda");
        vehicle.setModelName("CBR");
        vehicle.setStatus(status);
        vehicle.setSellingPrice(new BigDecimal("150000"));
        vehicle.setMainImageUrl("http://example.com/img.jpg");
        vehicle.setCreatedAt(LocalDateTime.now().minusDays(id));
        return vehicle;
    }

    private Enquiry createTestEnquiry(Long id, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry();
        enquiry.setId(id);
        enquiry.setStatus(status);
        enquiry.setCustomerName("Customer " + id);
        enquiry.setCreatedAt(LocalDateTime.now().minusHours(id));
        return enquiry;
    }

    private ShopReview createTestReview(Long id, int rating) {
        ShopReview review = new ShopReview();
        review.setId(id);
        review.setReviewerName("Reviewer " + id);
        review.setRating(rating);
        review.setComment("Great service");
        review.setCreatedAt(LocalDateTime.now().minusDays(id));
        return review;
    }

    @Test
    void getShopDashboardSummary_usesCountQueries_notFindAll() {
        Long shopId = 1L;

        when(vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE)).thenReturn(15L);
        when(enquiryRepository.countByShopIdAndStatus(shopId, EnquiryStatus.PENDING)).thenReturn(3L);
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(10L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(4.5);

        // Recent vehicles — 5 items
        List<Vehicle> vehicles = List.of(
                createTestVehicle(1L, VehicleStatus.ACTIVE),
                createTestVehicle(2L, VehicleStatus.ACTIVE),
                createTestVehicle(3L, VehicleStatus.SOLD),
                createTestVehicle(4L, VehicleStatus.ACTIVE),
                createTestVehicle(5L, VehicleStatus.INACTIVE)
        );
        when(vehicleRepository.findByShopIdWithDetails(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(vehicles));

        // Recent enquiries — 5 items
        List<Enquiry> enquiries = List.of(
                createTestEnquiry(1L, EnquiryStatus.PENDING),
                createTestEnquiry(2L, EnquiryStatus.CONTACTED),
                createTestEnquiry(3L, EnquiryStatus.CLOSED),
                createTestEnquiry(4L, EnquiryStatus.PENDING),
                createTestEnquiry(5L, EnquiryStatus.CONTACTED)
        );
        when(enquiryRepository.findByShopIdWithCustomerVehicleShop(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(enquiries));

        // Recent reviews — 5 items
        List<ShopReview> reviews = List.of(
                createTestReview(1L, 5),
                createTestReview(2L, 4),
                createTestReview(3L, 5),
                createTestReview(4L, 3),
                createTestReview(5L, 5)
        );
        when(shopReviewRepository.findByShopIdOrderByCreatedAtDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reviews));

        ShopDashboardSummaryDto result = dashboardService.getShopDashboardSummary(shopId);

        assertNotNull(result);

        // Verify vehicle counts
        assertEquals(15, result.getVehicleCounts().getAvailable());
        assertEquals(0, result.getVehicleCounts().getPublished()); // PUBLISHED doesn't exist in enum

        // Verify enquiry counts
        assertEquals(3, result.getEnquiryCounts().getPending());

        // Verify review summary
        assertEquals(10, result.getReviewSummary().getCount());
        assertEquals(4.5, result.getReviewSummary().getAverageRating(), 0.01);
        assertEquals(5, result.getReviewSummary().getRecentReviews().size());

        // Verify recent vehicles
        assertEquals(5, result.getRecentVehicles().size());
        assertEquals("Test Vehicle 1", result.getRecentVehicles().get(0).getTitle());
        assertEquals("Honda", result.getRecentVehicles().get(0).getBrand());
        assertEquals(new BigDecimal("150000"), result.getRecentVehicles().get(0).getSellPrice());

        // Verify recent enquiries
        assertEquals(5, result.getRecentEnquiries().size());
        assertEquals("Customer 1", result.getRecentEnquiries().get(0).getCustomerName());
        assertEquals("PENDING", result.getRecentEnquiries().get(0).getStatus());

        // Verify count queries were used (not findAll)
        verify(vehicleRepository).countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE);
        verify(enquiryRepository).countByShopIdAndStatus(shopId, EnquiryStatus.PENDING);
        verify(shopReviewRepository).countByShopId(shopId);
        verify(shopReviewRepository).getAverageRatingByShopId(shopId);

        // Verify paginated queries were used for recent items
        verify(vehicleRepository).findByShopIdWithDetails(eq(shopId), any(PageRequest.class));
        verify(enquiryRepository).findByShopIdWithCustomerVehicleShop(eq(shopId), any(PageRequest.class));
        verify(shopReviewRepository).findByShopIdOrderByCreatedAtDesc(eq(shopId), any(PageRequest.class));

        // Verify findAll was never called on any repository
        verify(vehicleRepository, never()).findAll();
        verify(enquiryRepository, never()).findAll();
        verify(shopReviewRepository, never()).findAll();
    }

    @Test
    void getShopDashboardSummary_withNoData_returnsZeros() {
        Long shopId = 2L;

        when(vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE)).thenReturn(0L);
        when(enquiryRepository.countByShopIdAndStatus(shopId, EnquiryStatus.PENDING)).thenReturn(0L);
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(0L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(0.0);
        when(vehicleRepository.findByShopIdWithDetails(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(enquiryRepository.findByShopIdWithCustomerVehicleShop(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(shopReviewRepository.findByShopIdOrderByCreatedAtDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ShopDashboardSummaryDto result = dashboardService.getShopDashboardSummary(shopId);

        assertNotNull(result);
        assertEquals(0, result.getVehicleCounts().getAvailable());
        assertEquals(0, result.getEnquiryCounts().getPending());
        assertEquals(0, result.getReviewSummary().getCount());
        assertEquals(0.0, result.getReviewSummary().getAverageRating(), 0.01);
        assertTrue(result.getRecentVehicles().isEmpty());
        assertTrue(result.getRecentEnquiries().isEmpty());
        assertTrue(result.getReviewSummary().getRecentReviews().isEmpty());
    }

    @Test
    void getShopDashboardSummary_nullEnquiryCount_handlesGracefully() {
        Long shopId = 3L;

        when(vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE)).thenReturn(5L);
        when(enquiryRepository.countByShopIdAndStatus(shopId, EnquiryStatus.PENDING)).thenReturn(null);
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(2L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(3.0);
        when(vehicleRepository.findByShopIdWithDetails(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(enquiryRepository.findByShopIdWithCustomerVehicleShop(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(shopReviewRepository.findByShopIdOrderByCreatedAtDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ShopDashboardSummaryDto result = dashboardService.getShopDashboardSummary(shopId);

        assertNotNull(result);
        assertEquals(0, result.getEnquiryCounts().getPending()); // null → 0
        assertEquals(5, result.getVehicleCounts().getAvailable());
    }

    @Test
    void getShopDashboardSummary_soldVehicle_markedAsSold() {
        Long shopId = 4L;

        when(vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE)).thenReturn(1L);
        when(enquiryRepository.countByShopIdAndStatus(shopId, EnquiryStatus.PENDING)).thenReturn(0L);
        when(shopReviewRepository.countByShopId(shopId)).thenReturn(0L);
        when(shopReviewRepository.getAverageRatingByShopId(shopId)).thenReturn(0.0);

        Vehicle soldVehicle = createTestVehicle(1L, VehicleStatus.SOLD);
        when(vehicleRepository.findByShopIdWithDetails(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(soldVehicle)));
        when(enquiryRepository.findByShopIdWithCustomerVehicleShop(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(shopReviewRepository.findByShopIdOrderByCreatedAtDesc(eq(shopId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ShopDashboardSummaryDto result = dashboardService.getShopDashboardSummary(shopId);

        assertEquals(1, result.getRecentVehicles().size());
        assertTrue(result.getRecentVehicles().get(0).isSold());
        assertEquals("SOLD", result.getRecentVehicles().get(0).getStatus());
    }
}
