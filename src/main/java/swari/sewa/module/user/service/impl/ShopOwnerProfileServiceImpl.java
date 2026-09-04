package swari.sewa.module.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.dto.ShopOwnerProfileDto;
import swari.sewa.module.user.dto.KycSubmissionDto;
import swari.sewa.module.user.dto.SubscriptionPlanDto;
import swari.sewa.module.user.service.ShopOwnerProfileService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopOwnerProfileServiceImpl implements ShopOwnerProfileService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final ShopReviewRepository shopReviewRepository;
    private final VehicleRepository vehicleRepository;
    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShopOwnerProfileDto getProfile() {
        ShopOwner shopOwner = getCurrentShopOwner();

        Shop shop = shopRepository.findByShopOwnerId(shopOwner.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (shop == null && "APPROVED".equals(shopOwner.getApprovalStatus())) {
            User user = userRepository.findByEmail(shopOwner.getEmail())
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(shopOwner.getEmail())
                            .password(shopOwner.getPassword())
                            .firstName(shopOwner.getFirstName() != null ? shopOwner.getFirstName() : "Shop")
                            .lastName(shopOwner.getLastName() != null ? shopOwner.getLastName() : "Owner")
                            .phoneNumber(shopOwner.getPhone())
                            .role(UserRole.SHOP_OWNER)
                            .isActive(true)
                            .isEmailVerified(true)
                            .build()));

            String licenseNumber = shopOwner.getLicenseNumber();
            if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
                licenseNumber = "SHOP-" + shopOwner.getId();
            }
            if (shopRepository.existsByLicenseNumber(licenseNumber)) {
                licenseNumber = licenseNumber + "-" + System.currentTimeMillis();
            }

            shop = Shop.builder()
                    .name((shopOwner.getShopName() != null && !shopOwner.getShopName().trim().isEmpty())
                            ? shopOwner.getShopName()
                            : (shopOwner.getCompanyName() != null && !shopOwner.getCompanyName().trim().isEmpty())
                                ? shopOwner.getCompanyName()
                                : (shopOwner.getFirstName() != null ? shopOwner.getFirstName() : "Shop") + " Shop")
                    .description("Auto-created shop for shop owner")
                    .licenseNumber(licenseNumber)
                    .addressLine1(shopOwner.getAddress())
                    .city(shopOwner.getCity() != null ? shopOwner.getCity() : "Unknown")
                    .state(shopOwner.getState() != null ? shopOwner.getState() : "Unknown")
                    .country(shopOwner.getCountry() != null ? shopOwner.getCountry() : "Unknown")
                    .phoneNumber(shopOwner.getShopPhone() != null ? shopOwner.getShopPhone() : shopOwner.getPhone())
                    .emailAddress(shopOwner.getShopEmail() != null ? shopOwner.getShopEmail() : shopOwner.getEmail())
                    .status(ShopStatus.ACTIVE)
                    .isFeatured(false)
                    .displayOrder(0)
                    .shopOwner(shopOwner)
                    .user(user)
                    .build();

            shop = shopRepository.save(shop);
        }

        Long shopId = shop.getId();
        
        return ShopOwnerProfileDto.builder()
                .id(shopOwner.getId())
                .shopId(shopId)
                .firstName(shopOwner.getFirstName())
                .lastName(shopOwner.getLastName())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhone())
                .companyName(shopOwner.getCompanyName())
                .licenseNumber(shopOwner.getLicenseNumber())
                .profilePhoto(shopOwner.getProfilePhoto())
                .shopLogo(shopOwner.getShopLogo())
                .fatherName(shopOwner.getFatherName())
                .citizenshipNo(shopOwner.getCitizenshipNo())
                .citizenshipPicFront(shopOwner.getCitizenshipPicFront())
                .citizenshipPicBack(shopOwner.getCitizenshipPicBack())
                .shopType(shopOwner.getShopType())
                .province(shopOwner.getProvince())
                .district(shopOwner.getDistrict())
                .municipality(shopOwner.getMunicipality())
                .ward(shopOwner.getWard())
                .tole(shopOwner.getTole())
                .shopPhone(shopOwner.getShopPhone())
                .shopEmail(shopOwner.getShopEmail())
                .pan(shopOwner.getPan())
                .regCert(shopOwner.getRegCert())
                .vat(shopOwner.getVat())
                .openingTime(shopOwner.getOpeningTime())
                .closingTime(shopOwner.getClosingTime())
                .offDays(shopOwner.getOffDays())
                .vehicleLimit(shopOwner.getVehicleLimit())
                .staffLimit(shopOwner.getStaffLimit())
                .citizenshipUpload(shopOwner.getCitizenshipUpload())
                .shopRegUpload(shopOwner.getShopRegUpload())
                .whatsappNo(shopOwner.getWhatsappNo())
                .facebookPage(shopOwner.getFacebookPage())
                .googleMapLink(shopOwner.getGoogleMapLink())
                .notes(shopOwner.getNotes())
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
        shopOwner.setLicenseNumber(profileDto.getLicenseNumber());
        shopOwner.setProfilePhoto(profileDto.getProfilePhoto());
        shopOwner.setShopLogo(profileDto.getShopLogo());
        shopOwner.setFatherName(profileDto.getFatherName());
        shopOwner.setCitizenshipNo(profileDto.getCitizenshipNo());
        shopOwner.setCitizenshipPicFront(profileDto.getCitizenshipPicFront());
        shopOwner.setCitizenshipPicBack(profileDto.getCitizenshipPicBack());
        shopOwner.setShopType(profileDto.getShopType());
        shopOwner.setProvince(profileDto.getProvince());
        shopOwner.setDistrict(profileDto.getDistrict());
        shopOwner.setMunicipality(profileDto.getMunicipality());
        shopOwner.setWard(profileDto.getWard());
        shopOwner.setTole(profileDto.getTole());
        shopOwner.setShopPhone(profileDto.getShopPhone());
        shopOwner.setShopEmail(profileDto.getShopEmail());
        shopOwner.setPan(profileDto.getPan());
        shopOwner.setRegCert(profileDto.getRegCert());
        shopOwner.setVat(profileDto.getVat());
        shopOwner.setOpeningTime(profileDto.getOpeningTime());
        shopOwner.setClosingTime(profileDto.getClosingTime());
        shopOwner.setOffDays(profileDto.getOffDays());
        shopOwner.setVehicleLimit(profileDto.getVehicleLimit());
        shopOwner.setStaffLimit(profileDto.getStaffLimit());
        shopOwner.setCitizenshipUpload(profileDto.getCitizenshipUpload());
        shopOwner.setShopRegUpload(profileDto.getShopRegUpload());
        shopOwner.setWhatsappNo(profileDto.getWhatsappNo());
        shopOwner.setFacebookPage(profileDto.getFacebookPage());
        shopOwner.setGoogleMapLink(profileDto.getGoogleMapLink());
        shopOwner.setNotes(profileDto.getNotes());
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
        long soldVehicles = vehicleRepository.countByShop_ShopOwner_IdAndStatus(shopOwner.getId(), swari.sewa.common.enums.VehicleStatus.SOLD);
        long totalEnquiries = enquiryRepository.countByShop_ShopOwner_Id(shopOwner.getId());
        long pendingEnquiries = enquiryRepository.countByShop_ShopOwner_IdAndStatus(shopOwner.getId(), swari.sewa.common.enums.EnquiryStatus.PENDING);

        // Aggregate rating across ALL shops owned by this shop owner in a SINGLE query.
        // Before: N+1 loop — for each shop: 2 queries (getAverageRatingByShopId + countByShopId)
        // After: 1 aggregate query (COUNT + SUM joined with Shop on shopOwner.id)
        Object[] ratingAgg = shopReviewRepository.aggregateRatingByShopOwnerId(shopOwner.getId());
        long ratingCount = 0;
        double ratingSum = 0.0;
        if (ratingAgg != null && ratingAgg.length >= 2) {
            ratingCount = ratingAgg[0] instanceof Number ? ((Number) ratingAgg[0]).longValue() : 0L;
            ratingSum = ratingAgg[1] instanceof Number ? ((Number) ratingAgg[1]).doubleValue() : 0.0;
        }
        double averageRating = ratingCount > 0 ? Math.round((ratingSum / ratingCount) * 10.0) / 10.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShops", totalShops);
        stats.put("activeShops", activeShops);
        stats.put("totalVehicles", totalVehicles);
        stats.put("activeVehicles", activeVehicles);
        stats.put("soldVehicles", soldVehicles);
        stats.put("rating", averageRating);
        stats.put("ratingCount", ratingCount);
        stats.put("totalEnquiries", totalEnquiries);
        stats.put("pendingEnquiries", pendingEnquiries);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBookings(int page, int size) {
        ShopOwner shopOwner = getCurrentShopOwner();
        Pageable pageable = PageRequest.of(page, size);

        // Use JOIN FETCH variant to eliminate N+1 lazy loading of customer/vehicle.
        // Before: findByShop_ShopOwner_Id → each enquiry.getCustomer()/getVehicle() triggers a separate query
        // After: findByShopOwner_IdWithCustomerVehicleShop → customer/vehicle fetched in a single JOIN query
        Page<Enquiry> enquiries = enquiryRepository.findByShopOwner_IdWithCustomerVehicleShop(shopOwner.getId(), pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("bookings", enquiries.map(enquiry -> {
            Map<String, Object> booking = new HashMap<>();
            booking.put("id", enquiry.getId());
            booking.put("customerName", enquiry.getCustomer() != null
                    ? enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName()
                    : enquiry.getCustomerName());
            booking.put("customerEmail", enquiry.getCustomer() != null ? enquiry.getCustomer().getEmail() : enquiry.getCustomerEmail());
            booking.put("customerPhone", enquiry.getCustomer() != null ? enquiry.getCustomer().getPhone() : null);
            booking.put("vehicleTitle", enquiry.getVehicle() != null ? enquiry.getVehicle().getTitle() : null);
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

    @Override
    public void updateProfilePhoto(String photoUrl) {
        ShopOwner shopOwner = getCurrentShopOwner();
        shopOwner.setProfilePhoto(photoUrl);
        shopOwnerRepository.save(shopOwner);
    }

    private ShopOwner getCurrentShopOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        return shopOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Shop owner not found with email: " + email));
    }
}
