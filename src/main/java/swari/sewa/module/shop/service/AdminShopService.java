package swari.sewa.module.shop.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminShopService {
    
    Page<Object> getAllShops(Pageable pageable, String search, String status);
    
    Object getShopById(Long id);
    
    void approveShop(Long id);
    
    void rejectShop(Long id);
    
    void suspendShop(Long id);
    
    void reactivateShop(Long id);
    
    void deleteShop(Long id);
    
    Page<Object> getPendingShops(Pageable pageable);
}
