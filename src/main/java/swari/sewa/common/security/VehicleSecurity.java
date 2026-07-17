package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;

@Component("vehicleSecurity")
@RequiredArgsConstructor
public class VehicleSecurity {

    private final VehicleRepository vehicleRepository;
    private final ShopRepository shopRepository;

    /**
     * Check if the authenticated shop owner (by email) owns the vehicle with the given vehicle ID.
     * 
     * @param vehicleId The vehicle ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the vehicle, false otherwise
     */
    public boolean isShopOwner(Long vehicleId, String email) {
        if (vehicleId == null || email == null) {
            return false;
        }

        try {
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                return false;
            }

            Shop shop = vehicle.getShop();
            if (shop == null || shop.getShopOwner() == null) {
                return false;
            }

            // Compare the shop owner's email with the authenticated email
            return email.equals(shop.getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }
}
