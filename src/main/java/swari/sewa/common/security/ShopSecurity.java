package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.wishlist.entity.Wishlist;
import swari.sewa.module.wishlist.repository.WishlistRepository;

@Component("shopSecurity")
@RequiredArgsConstructor
public class ShopSecurity {

    private final ShopRepository shopRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final WishlistRepository wishlistRepository;

    /**
     * Check if the authenticated shop owner (by email) owns the shop with the given shop ID.
     * 
     * @param shopId The shop ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the shop, false otherwise
     */
    public boolean isOwner(Long shopId, String email) {
        if (shopId == null || email == null) {
            return false;
        }

        try {
            Shop shop = shopRepository.findById(shopId).orElse(null);
            if (shop == null) {
                return false;
            }

            // Compare the shop owner's email with the authenticated email
            return email.equals(shop.getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns enquiries with the given status.
     * This is used for filtering enquiries by status for a shop owner.
     * 
     * @param status The enquiry status (not used for ownership check, but required by @PreAuthorize)
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated user is a shop owner, false otherwise
     */
    public boolean isShopOwnerByStatus(String status, String email) {
        if (email == null) {
            return false;
        }

        try {
            ShopOwner shopOwner = shopOwnerRepository.findByEmail(email).orElse(null);
            return shopOwner != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the vehicle that
     * is referenced by the given wishlist entry.
     *
     * @param wishlistId The wishlist ID whose vehicle's shop owner should be checked
     * @param email The authenticated shop owner's email
     * @return true if the shop owner owns the vehicle's shop, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isWishlistOwner(Long wishlistId, String email) {
        if (wishlistId == null || email == null) {
            return false;
        }

        try {
            Wishlist wishlist = wishlistRepository.findById(wishlistId).orElse(null);
            if (wishlist == null || wishlist.getVehicle() == null || wishlist.getVehicle().getShop() == null) {
                return false;
            }

            Long shopId = wishlist.getVehicle().getShop().getId();
            return isOwner(shopId, email);
        } catch (Exception e) {
            return false;
        }
    }
}
