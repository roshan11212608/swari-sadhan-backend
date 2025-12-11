package swari.sewa.module.shopowner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shopowner.model.ShopOwner;
import swari.sewa.module.shopowner.repository.ShopOwnerRepository;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.shopowner.repository.ShopRepository;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.module.publicuser.model.Enquiry;
import swari.sewa.module.publicuser.repository.EnquiryRepository;
import swari.sewa.module.shopowner.dto.ShopOwnerProfileDto;
import swari.sewa.module.shopowner.dto.KycSubmissionDto;
import swari.sewa.module.shopowner.dto.SubscriptionPlanDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopOwnerProfileServiceImpl implements ShopOwnerProfileService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;
    private final EnquiryRepository enquiryRepository;

    @Override
    @Transactional(readOnly = true)
    public ShopOwnerProfileDto getProfile() {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        return ShopOwnerProfileDto.builder()
                .id(shopOwner.getId())
                .firstName(shopOwner.getFirstName())
                .lastName(shopOwner.getLastName())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhone())
                .companyName(shopOwner.getCompanyName())
                .licenseNumber(shopOwner.getLicenseNumber())
                .address(shopOwner.getAddress())
                .city(shopOwner.getCity())
                .state(shopOwner.getState())
                .postalCode(shopOwner.getPostalCode())
                .country(shopOwner.getCountry())
                .website(shopOwner.getWebsite())
                .description(shopOwner.getDescription())
                .kycVerified(shopOwner.getKycVerified())
                .subscriptionActive(shopOwner.getSubscriptionActive())
                .subscriptionPlan(shopOwner.getSubscriptionPlan())
                .subscriptionExpiresAt(shopOwner.getSubscriptionExpiresAt())
                .createdAt(shopOwner.getCreatedAt())
                .build();
    }

    @Override
    public ShopOwnerProfileDto updateProfile(ShopOwnerProfileDto profileDto) {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        shopOwner.setFirstName(profileDto.getFirstName());
        shopOwner.setLastName(profileDto.getLastName());
        shopOwner.setPhone(profileDto.getPhone());
        shopOwner.setCompanyName(profileDto.getCompanyName());
        shopOwner.setAddress(profileDto.getAddress());
        shopOwner.setCity(profileDto.getCity());
        shopOwner.setState(profileDto.getState());
        shopOwner.setPostalCode(profileDto.getPostalCode());
        shopOwner.setCountry(profileDto.getCountry());
        shopOwner.setWebsite(profileDto.getWebsite());
        shopOwner.setDescription(profileDto.getDescription());
        
        shopOwnerRepository.save(shopOwner);
        
        return getProfile();
    }

    @Override
    public void submitKyc(KycSubmissionDto kycDto) {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        shopOwner.setLicenseNumber(kycDto.getLicenseNumber());
        shopOwner.setAddress(kycDto.getAddress());
        shopOwner.setCity(kycDto.getCity());
        shopOwner.setState(kycDto.getState());
        shopOwner.setPostalCode(kycDto.getPostalCode());
        shopOwner.setCountry(kycDto.getCountry());
        
        shopOwnerRepository.save(shopOwner);
        
        // TODO: Implement actual KYC verification process
        // For now, mark as verified after submission
        shopOwner.setKycVerified(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getKycStatus() {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        Map<String, Object> kycStatus = new HashMap<>();
        kycStatus.put("verified", shopOwner.getKycVerified());
        kycStatus.put("submitted", shopOwner.getLicenseNumber() != null);
        kycStatus.put("licenseNumber", shopOwner.getLicenseNumber());
        
        return kycStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto getSubscriptionPlan() {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        return SubscriptionPlanDto.builder()
                .currentPlan(shopOwner.getSubscriptionPlan() != null ? shopOwner.getSubscriptionPlan() : "FREE")
                .active(shopOwner.getSubscriptionActive())
                .expiresAt(shopOwner.getSubscriptionExpiresAt())
                .build();
    }

    @Override
    public void upgradeSubscription(String plan) {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        shopOwner.setSubscriptionPlan(plan);
        shopOwner.setSubscriptionActive(true);
        
        // Set expiration based on plan
        LocalDateTime expiresAt = LocalDateTime.now();
        switch (plan.toUpperCase()) {
            case "BASIC":
                expiresAt = expiresAt.plusMonths(1);
                break;
            case "PREMIUM":
                expiresAt = expiresAt.plusMonths(3);
                break;
            case "ENTERPRISE":
                expiresAt = expiresAt.plusYears(1);
                break;
            default:
                expiresAt = expiresAt.plusMonths(1);
        }
        
        shopOwner.setSubscriptionExpiresAt(expiresAt);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        long totalShops = shopRepository.countByShopOwner_Id(shopOwner.getId());
        long activeShops = shopRepository.countByShopOwner_IdAndStatus(shopOwner.getId(), swari.sewa.common.enums.ShopStatus.ACTIVE);
        long totalVehicles = vehicleRepository.countByShop_ShopOwner_Id(shopOwner.getId());
        long activeVehicles = vehicleRepository.countByShop_ShopOwner_IdAndStatus(shopOwner.getId(), swari.sewa.common.enums.VehicleStatus.ACTIVE);
        long totalEnquiries = enquiryRepository.countByShop_ShopOwner_Id(shopOwner.getId());
        long pendingEnquiries = enquiryRepository.countByShop_ShopOwner_IdAndStatus(shopOwner.getId(), swari.sewa.common.enums.EnquiryStatus.PENDING);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShops", totalShops);
        stats.put("activeShops", activeShops);
        stats.put("totalVehicles", totalVehicles);
        stats.put("activeVehicles", activeVehicles);
        stats.put("totalEnquiries", totalEnquiries);
        stats.put("pendingEnquiries", pendingEnquiries);
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBookings(int page, int size) {
        ShopOwner shopOwner = getCurrentShopOwner();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Enquiry> enquiries = enquiryRepository.findByShop_ShopOwner_Id(shopOwner.getId(), pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("bookings", enquiries.map(enquiry -> {
            Map<String, Object> booking = new HashMap<>();
            booking.put("id", enquiry.getId());
            booking.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
            booking.put("customerEmail", enquiry.getCustomer().getEmail());
            booking.put("customerPhone", enquiry.getCustomer().getPhone());
            booking.put("vehicleTitle", enquiry.getVehicle().getTitle());
            booking.put("status", enquiry.getStatus());
            booking.put("message", enquiry.getMessage());
            booking.put("createdAt", enquiry.getCreatedAt());
            return booking;
        }));
        result.put("totalPages", enquiries.getTotalPages());
        result.put("totalElements", enquiries.getTotalElements());
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomers(int page, int size) {
        ShopOwner shopOwner = getCurrentShopOwner();
        
        // TODO: Implement customer management logic
        Map<String, Object> result = new HashMap<>();
        result.put("customers", new Object[]{});
        result.put("totalPages", 0);
        result.put("totalElements", 0);
        
        return result;
    }

    private ShopOwner getCurrentShopOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        return shopOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
    }
}
