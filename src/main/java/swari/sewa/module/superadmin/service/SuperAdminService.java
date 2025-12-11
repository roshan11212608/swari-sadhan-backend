package swari.sewa.module.superadmin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.superadmin.dto.DashboardStatsDto;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;
import swari.sewa.module.superadmin.dto.UserManagementDto;

public interface SuperAdminService {
    
    DashboardStatsDto getDashboardStats();
    
    Page<ShopOwnerDto> getShopOwners(Pageable pageable);
    
    Page<UserManagementDto> getUsers(Pageable pageable);
    
    void activateShopOwner(Long id);
    
    void deactivateShopOwner(Long id);
    
    void activateUser(Long id);
    
    void deactivateUser(Long id);
    
    Object getReportsSummary();
    
    Page<Object> getVehicleReports(Pageable pageable);
    
    Page<Object> getEnquiryReports(Pageable pageable);
}
