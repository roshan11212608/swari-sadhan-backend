package swari.sewa.module.superadmin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;

public interface ShopOwnerService {
    
    Page<ShopOwnerDto> getAllShopOwners(Pageable pageable, String search, String status);
    
    ShopOwnerDto getShopOwnerById(Long id);
    
    void approveShopOwner(Long id);
    
    void rejectShopOwner(Long id);
    
    void suspendShopOwner(Long id);
    
    void reactivateShopOwner(Long id);
    
    Page<Object> getShopOwnerShops(Long id, Pageable pageable);
    
    Page<Object> getShopOwnerVehicles(Long id, Pageable pageable);
}
