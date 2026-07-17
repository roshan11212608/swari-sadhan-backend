package swari.sewa.module.shop.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import swari.sewa.module.shop.dto.ShopDto;

public interface ShopService {
    
    ShopDto createShop(ShopDto shopDto, Long userId);
    
    Optional<ShopDto> getShopById(Long id);
    
    Optional<ShopDto> getShopByUserId(Long userId);
    
    List<ShopDto> getAllShops();
    
    Page<ShopDto> getAllShops(Pageable pageable);
    
    List<ShopDto> getShopsByCity(String city);
    
    Page<ShopDto> getShopsByCity(String city, Pageable pageable);
    
    List<ShopDto> getShopsByState(String state);
    
    Page<ShopDto> getShopsByState(String state, Pageable pageable);
    
    List<ShopDto> getFeaturedShops();
    
    Page<ShopDto> getFeaturedShops(Pageable pageable);
    
    ShopDto updateShop(Long id, ShopDto shopDto);
    
    void deleteShop(Long id);
    
    ShopDto approveShop(Long id);
    
    ShopDto rejectShop(Long id);
    
    ShopDto activateShop(Long id);
    
    ShopDto deactivateShop(Long id);
    
    ShopDto suspendShop(Long id);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    List<ShopDto> searchShops(String keyword);
    
    Page<ShopDto> searchShops(String keyword, Pageable pageable);
}
