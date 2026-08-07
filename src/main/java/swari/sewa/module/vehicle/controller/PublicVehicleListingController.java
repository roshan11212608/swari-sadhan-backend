package swari.sewa.module.vehicle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.dto.PublicVehicleListingRequestDto;
import swari.sewa.module.vehicle.dto.PublicVehicleListingResponseDto;
import swari.sewa.module.vehicle.dto.PublicVehicleListingSellerDto;
import swari.sewa.module.vehicle.service.PublicVehicleListingService;

import java.util.Map;

@RestController
@RequestMapping("/api/public/vehicle-listings")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        maxAge = 3600
)
public class PublicVehicleListingController {

    private final PublicVehicleListingService listingService;
    private final UserRepository userRepository;

    /* ===================== HELPERS ===================== */

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    private ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Request failed",
                "message", e.getMessage()
        ));
    }

    /* ===================== PUBLIC MARKETPLACE ===================== */

    @GetMapping
    public ResponseEntity<?> getPublicListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PublicVehicleListingResponseDto> listings = listingService.getPublicListings(pageable);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicListing(@PathVariable Long id) {
        try {
            PublicVehicleListingResponseDto listing = listingService.getPublicListing(id);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /* ===================== SELLER SUBMISSION ===================== */

    @PostMapping
    public ResponseEntity<?> createListing(
            @RequestBody PublicVehicleListingRequestDto dto,
            @RequestParam(defaultValue = "false") boolean draft) {
        try {
            Long sellerUserId = resolveCurrentUserId();
            PublicVehicleListingSellerDto created = listingService.createListing(dto, sellerUserId, draft);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /* ===================== SELLER MANAGEMENT ===================== */

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Long sellerUserId = resolveCurrentUserId();
            if (sellerUserId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Pageable pageable = PageRequest.of(page, size);
            Page<PublicVehicleListingSellerDto> listings = listingService.getListingsForSeller(sellerUserId, status, pageable);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyListing(@PathVariable Long id) {
        try {
            Long sellerUserId = resolveCurrentUserId();
            if (sellerUserId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            PublicVehicleListingSellerDto listing = listingService.getListingForSeller(id, sellerUserId);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/my/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateMyListing(
            @PathVariable Long id,
            @RequestBody PublicVehicleListingRequestDto dto) {
        try {
            Long sellerUserId = resolveCurrentUserId();
            if (sellerUserId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            PublicVehicleListingSellerDto updated = listingService.updateListing(id, dto, sellerUserId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/my/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelMyListing(@PathVariable Long id) {
        try {
            Long sellerUserId = resolveCurrentUserId();
            if (sellerUserId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            listingService.deleteListing(id, sellerUserId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
