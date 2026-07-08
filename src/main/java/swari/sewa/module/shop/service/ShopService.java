package swari.sewa.module.shop.service;

import java.util.List;
import java.util.Optional;

import swari.sewa.module.shop.dto.ShopDto;

public interface ShopService {
    
    ShopDto createShop(ShopDto shopDto, Long userId);
    
    Optional<ShopDto> getShopById(Long id);
    
    Optional<ShopDto> getShopByUserId(Long userId);
    
    List<ShopDto> getAllShops();
    
    List<ShopDto> getShopsByCity(String city);
    
    List<ShopDto> getShopsByState(String state);
    
    List<ShopDto> getFeaturedShops();
    
    ShopDto updateShop(Long id, ShopDto shopDto);
    
    void deleteShop(Long id);
    
    ShopDto approveShop(Long id);
    
    ShopDto rejectShop(Long id);
    
    ShopDto activateShop(Long id);
    
    ShopDto deactivateShop(Long id);
    
    ShopDto suspendShop(Long id);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    List<ShopDto> searchShops(String keyword);
}
