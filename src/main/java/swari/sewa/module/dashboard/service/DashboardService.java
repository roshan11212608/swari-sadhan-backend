package swari.sewa.module.dashboard.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.dashboard.dto.DashboardStatsDto;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.dashboard.dto.UserManagementDto;
import swari.sewa.module.dashboard.dto.CredentialsDto;
import swari.sewa.module.dashboard.dto.ShopDashboardSummaryDto;

public interface DashboardService {

    ShopDashboardSummaryDto getShopDashboardSummary(Long shopId);
    
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
    
    // Credentials Management Methods
    Page<CredentialsDto> getCredentials(Pageable pageable, String userType, String status, String search);
    
    CredentialsDto getCredentialById(Long id);
    
    String resetUserPassword(Long id);
    
    void toggleCredentialStatus(Long id);
    
    void deleteCredential(Long id);
}
