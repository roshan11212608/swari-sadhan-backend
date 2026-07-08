package swari.sewa.module.wishlist.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.wishlist.dto.WishlistDto;
import swari.sewa.module.wishlist.service.WishlistService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<WishlistDto> addToWishlist(
            @RequestParam Long customerId,
            @RequestParam Long vehicleId) {
        WishlistDto wishlistItem = wishlistService.addToWishlist(customerId, vehicleId);
        return ResponseEntity.ok(wishlistItem);
    }

    @DeleteMapping("/remove")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestParam Long customerId,
            @RequestParam Long vehicleId) {
        wishlistService.removeFromWishlist(customerId, vehicleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<WishlistDto> getWishlistById(@PathVariable Long id) {
        Optional<WishlistDto> wishlist = wishlistService.getWishlistById(id);
        return wishlist.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<WishlistDto>> getCustomerWishlist(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WishlistDto> wishlist = wishlistService.getCustomerWishlist(customerId, page, size);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/customer/{customerId}/all")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<WishlistDto>> getCustomerWishlistAll(@PathVariable Long customerId) {
        List<WishlistDto> wishlist = wishlistService.getCustomerWishlist(customerId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/check")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Boolean> isInWishlist(
            @RequestParam Long customerId,
            @RequestParam Long vehicleId) {
        boolean isInWishlist = wishlistService.isInWishlist(customerId, vehicleId);
        return ResponseEntity.ok(isInWishlist);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteWishlist(@PathVariable Long id) {
        wishlistService.deleteWishlist(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}/count")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getWishlistCount(@PathVariable Long customerId) {
        Long count = wishlistService.getWishlistCount(customerId);
        return ResponseEntity.ok(count);
    }
}
