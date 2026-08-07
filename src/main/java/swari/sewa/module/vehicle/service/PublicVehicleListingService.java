package swari.sewa.module.vehicle.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.vehicle.dto.*;

import java.util.List;

public interface PublicVehicleListingService {

    PublicVehicleListingSellerDto createListing(PublicVehicleListingRequestDto dto, Long sellerUserId, boolean draft);

    PublicVehicleListingSellerDto updateListing(Long id, PublicVehicleListingRequestDto dto, Long sellerUserId);

    PublicVehicleListingSellerDto getListingForSeller(Long id, Long sellerUserId);

    Page<PublicVehicleListingSellerDto> getListingsForSeller(Long sellerUserId, String status, Pageable pageable);

    void deleteListing(Long id, Long sellerUserId);

    PublicVehicleListingResponseDto getPublicListing(Long id);

    Page<PublicVehicleListingResponseDto> getPublicListings(Pageable pageable);

    PublicVehicleListingAdminDto getListingForAdmin(Long id);

    Page<PublicVehicleListingAdminDto> getListingsForAdmin(String status, String search, Pageable pageable);

    PublicVehicleListingAdminDto approveListing(Long id, PublicVehicleListingActionDto action);

    PublicVehicleListingAdminDto rejectListing(Long id, PublicVehicleListingActionDto action);

    PublicVehicleListingAdminDto requestChanges(Long id, PublicVehicleListingActionDto action);

    PublicVehicleListingAdminDto markAsSold(Long id, PublicVehicleListingActionDto action);

    PublicVehicleListingAdminDto underReviewListing(Long id);

    List<String> getAllActiveVehicleNumbers();
}
