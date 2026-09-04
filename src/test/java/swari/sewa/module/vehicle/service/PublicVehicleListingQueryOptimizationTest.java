package swari.sewa.module.vehicle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swari.sewa.common.enums.PublicVehicleListingStatus;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.dto.PublicVehicleListingAdminDto;
import swari.sewa.module.vehicle.entity.PublicVehicleListing;
import swari.sewa.module.vehicle.repository.PublicVehicleListingFileRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingReviewHistoryRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingSequenceRepository;
import swari.sewa.module.vehicle.service.impl.PublicVehicleListingServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PublicVehicleListingServiceImpl query optimization.
 *
 * <p>Verifies that toAdminDto (via getListingForAdmin) performs a SINGLE
 * userRepository.findById call instead of 3 separate calls.
 */
@ExtendWith(MockitoExtension.class)
class PublicVehicleListingQueryOptimizationTest {

    @Mock private PublicVehicleListingRepository listingRepository;
    @Mock private PublicVehicleListingFileRepository fileRepository;
    @Mock private PublicVehicleListingSequenceRepository sequenceRepository;
    @Mock private PublicVehicleListingReviewHistoryRepository reviewHistoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PublicVehicleListingServiceImpl listingService;

    @Test
    void getListingForAdmin_callsUserRepositoryOnceNotThreeTimes() {
        Long sellerUserId = 42L;
        PublicVehicleListing listing = PublicVehicleListing.builder()
                .id(1L)
                .listingNumber("SVL-202401-000001")
                .sellerUserId(sellerUserId)
                .status(PublicVehicleListingStatus.PUBLISHED)
                .build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        User seller = User.builder()
                .id(sellerUserId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("9801234567")
                .build();
        when(userRepository.findById(sellerUserId)).thenReturn(Optional.of(seller));

        PublicVehicleListingAdminDto result = listingService.getListingForAdmin(1L);

        assertNotNull(result);
        assertEquals("John Doe", result.getSellerAccountName());
        assertEquals("9801234567", result.getSellerAccountPhone());
        assertEquals("john@example.com", result.getSellerEmail());

        // CRITICAL: verify only ONE userRepository.findById call, not 3
        verify(userRepository, times(1)).findById(sellerUserId);
    }
}
