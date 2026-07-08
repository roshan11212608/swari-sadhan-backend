package swari.sewa.module.user.service;

import swari.sewa.module.user.dto.ShopOwnerProfileDto;
import swari.sewa.module.user.dto.KycSubmissionDto;
import swari.sewa.module.user.dto.SubscriptionPlanDto;

import java.util.Map;

public interface ShopOwnerProfileService {
    
    ShopOwnerProfileDto getProfile();
    
    ShopOwnerProfileDto updateProfile(ShopOwnerProfileDto profileDto);
    
    void submitKyc(KycSubmissionDto kycDto);
    
    Object getKycStatus();
    
    SubscriptionPlanDto getSubscriptionPlan();
    
    void upgradeSubscription(String plan);
    
    Map<String, Object> getDashboardStats();
    
    Map<String, Object> getBookings(int page, int size);
    
    Map<String, Object> getCustomers(int page, int size);
}
