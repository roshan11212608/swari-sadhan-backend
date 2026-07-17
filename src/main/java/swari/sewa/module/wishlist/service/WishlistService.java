package swari.sewa.module.wishlist.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import swari.sewa.module.wishlist.dto.WishlistDto;

public interface WishlistService {
    
    WishlistDto addToWishlist(Long customerId, Long vehicleId);
    
    void removeFromWishlist(Long customerId, Long vehicleId);
    
    Optional<WishlistDto> getWishlistById(Long id);
    
    Page<WishlistDto> getCustomerWishlist(Long customerId, int page, int size);
    
    List<WishlistDto> getCustomerWishlist(Long customerId);
    
    List<WishlistDto> getShopWishlist(Long shopId);
    
    boolean isInWishlist(Long customerId, Long vehicleId);
    
    void deleteWishlist(Long id);
    
    Long getWishlistCount(Long customerId);
}
