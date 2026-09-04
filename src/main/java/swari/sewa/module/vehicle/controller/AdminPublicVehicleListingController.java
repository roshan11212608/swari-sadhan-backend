package swari.sewa.module.vehicle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.vehicle.dto.PublicVehicleListingActionDto;
import swari.sewa.module.vehicle.dto.PublicVehicleListingAdminDto;
import swari.sewa.module.vehicle.service.PublicVehicleListingService;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/public-vehicle-listings")
@RequiredArgsConstructor
public class AdminPublicVehicleListingController {

    private final PublicVehicleListingService listingService;

    private ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Request failed",
                "message", e.getMessage()
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> getAdminListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PublicVehicleListingAdminDto> listings = listingService.getListingsForAdmin(status, search, pageable);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> getAdminListing(@PathVariable Long id) {
        try {
            PublicVehicleListingAdminDto listing = listingService.getListingForAdmin(id);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/under-review")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> underReview(@PathVariable Long id) {
        try {
            PublicVehicleListingAdminDto listing = listingService.underReviewListing(id);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestBody(required = false) PublicVehicleListingActionDto action) {
        try {
            PublicVehicleListingAdminDto listing = listingService.approveListing(id, action != null ? action : new PublicVehicleListingActionDto());
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody PublicVehicleListingActionDto action) {
        try {
            if (action == null || action.getReason() == null || action.getReason().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
            }
            PublicVehicleListingAdminDto listing = listingService.rejectListing(id, action);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/request-changes")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> requestChanges(
            @PathVariable Long id,
            @RequestBody PublicVehicleListingActionDto action) {
        try {
            if (action == null || action.getReason() == null || action.getReason().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Reason for changes is required"));
            }
            PublicVehicleListingAdminDto listing = listingService.requestChanges(id, action);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/mark-sold")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> markAsSold(
            @PathVariable Long id,
            @RequestBody(required = false) PublicVehicleListingActionDto action) {
        try {
            PublicVehicleListingAdminDto listing = listingService.markAsSold(id, action != null ? action : new PublicVehicleListingActionDto());
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> unpublish(
            @PathVariable Long id,
            @RequestBody(required = false) PublicVehicleListingActionDto action) {
        try {
            PublicVehicleListingAdminDto listing = listingService.unpublishListing(id, action != null ? action : new PublicVehicleListingActionDto());
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
