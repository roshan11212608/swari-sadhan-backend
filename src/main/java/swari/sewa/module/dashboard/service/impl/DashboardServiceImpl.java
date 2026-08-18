package swari.sewa.module.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.dashboard.dto.DashboardStatsDto;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.dashboard.dto.UserManagementDto;
import swari.sewa.module.dashboard.dto.CredentialsDto;
import swari.sewa.module.dashboard.service.DashboardService;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.util.PasswordGeneratorUtil;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;
    private final EnquiryRepository enquiryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalShopOwners = shopOwnerRepository.count();
        long activeShopOwners = shopOwnerRepository.countByActive(true);
        long totalShops = shopRepository.count();
        long activeShops = shopRepository.countByStatus(ShopStatus.ACTIVE);
        long totalVehicles = vehicleRepository.count();
        long activeVehicles = vehicleRepository.countByStatus(VehicleStatus.ACTIVE);
        long totalUsers = userRepository.countByRole(UserRole.PUBLIC);
        long totalEnquiries = enquiryRepository.count();
        long pendingEnquiries = enquiryRepository.countByStatus(swari.sewa.common.enums.EnquiryStatus.PENDING);

        return DashboardStatsDto.builder()
                .totalShopOwners(totalShopOwners)
                .activeShopOwners(activeShopOwners)
                .totalShops(totalShops)
                .activeShops(activeShops)
                .totalVehicles(totalVehicles)
                .activeVehicles(activeVehicles)
                .totalUsers(totalUsers)
                .totalEnquiries(totalEnquiries)
                .pendingEnquiries(pendingEnquiries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOwnerDto> getShopOwners(Pageable pageable) {
        return shopOwnerRepository.findAll(pageable)
                .map(shopOwner -> ShopOwnerDto.builder()
                        .id(shopOwner.getId())
                        .firstName(shopOwner.getFirstName())
                        .lastName(shopOwner.getLastName())
                        .email(shopOwner.getEmail())
                        .phone(shopOwner.getPhone())
                        .companyName(shopOwner.getCompanyName())
                        .active(shopOwner.isActive())
                        .createdAt(shopOwner.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementDto> getUsers(Pageable pageable) {
        return userRepository.findByRole(UserRole.PUBLIC, pageable)
                .map(user -> UserManagementDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    @Override
    public void activateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void deactivateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getReportsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", 0L); // TODO: Implement revenue calculation
        summary.put("monthlyRevenue", 0L); // TODO: Implement monthly revenue
        summary.put("totalTransactions", 0L); // TODO: Implement transaction count
        summary.put("activeSubscriptions", 0L); // TODO: Implement subscription count
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getVehicleReports(Pageable pageable) {
        // TODO: Implement detailed vehicle reports
        return vehicleRepository.findAll(pageable)
                .map(vehicle -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("id", vehicle.getId());
                    report.put("title", vehicle.getTitle());
                    report.put("status", vehicle.getStatus());
                    report.put("views", vehicle.getViewCount());
                    report.put("contacts", vehicle.getContactCount());
                    report.put("createdAt", vehicle.getCreatedAt());
                    return report;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getEnquiryReports(Pageable pageable) {
        return enquiryRepository.findAll(pageable)
                .map(enquiry -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("id", enquiry.getId());
                    report.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
                    report.put("vehicleTitle", enquiry.getVehicle().getTitle());
                    report.put("status", enquiry.getStatus());
                    report.put("createdAt", enquiry.getCreatedAt());
                    return report;
                });
    }

    // Credentials Management Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public Page<CredentialsDto> getCredentials(Pageable pageable, String userType, String status, String search) {
        // Combine shop owners and users
        java.util.List<CredentialsDto> allCredentials = new java.util.ArrayList<>();
        
        // Get all users once
        java.util.List<User> allUsers = userRepository.findAll();
        
        // Add superadmin users
        allUsers.forEach(user -> {
            if (user.getRole() == UserRole.SUPERADMIN && (userType == null || userType.equals("SUPERADMIN"))) {
                CredentialsDto credential = CredentialsDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .password("********") // Masked password
                        .role("SUPERADMIN")
                        .userType("SUPERADMIN")
                        .status(user.isActive() ? "ACTIVE" : "INACTIVE")
                        .createdAt(user.getCreatedAt())
                        .name(user.getFirstName() + " " + user.getLastName())
                        .phone(user.getPhone())
                        .shopName(null)
                        .isEmailVerified(user.isEmailVerified())
                        .build();
                
                // Apply filters
                if ((status == null || status.equals(credential.getStatus())) &&
                    (search == null || search.isEmpty() || 
                     credential.getEmail().toLowerCase().contains(search.toLowerCase()) ||
                     credential.getName().toLowerCase().contains(search.toLowerCase()))) {
                    allCredentials.add(credential);
                }
            }
        });
        
        // Add shop owners
        shopOwnerRepository.findAll().forEach(shopOwner -> {
            if (userType == null || userType.equals("SHOP_OWNER")) {
                CredentialsDto credential = CredentialsDto.builder()
                        .id(shopOwner.getId())
                        .email(shopOwner.getEmail())
                        .password("********") // Masked password
                        .role("SHOP_OWNER")
                        .userType("SHOP_OWNER")
                        .status(shopOwner.isActive() ? "ACTIVE" : "INACTIVE")
                        .approvalStatus(shopOwner.getApprovalStatus() != null ? shopOwner.getApprovalStatus() : "APPROVED")
                        .rejectionReason(shopOwner.getRejectionReason())
                        .createdAt(shopOwner.getCreatedAt())
                        .name(shopOwner.getFirstName() + " " + shopOwner.getLastName())
                        .phone(shopOwner.getPhone())
                        .shopName(shopOwner.getCompanyName())
                        .isEmailVerified(true) // Assuming shop owners are verified
                        // Owner Information
                        .firstName(shopOwner.getFirstName())
                        .lastName(shopOwner.getLastName())
                        .fatherName(shopOwner.getFatherName())
                        .address(shopOwner.getAddress())
                        .citizenshipNo(shopOwner.getCitizenshipNo())
                        .profilePhoto(shopOwner.getProfilePhoto())
                        .citizenshipPicFront(shopOwner.getCitizenshipPicFront())
                        .citizenshipPicBack(shopOwner.getCitizenshipPicBack())
                        // Location fields
                        .province(shopOwner.getProvince())
                        .district(shopOwner.getDistrict())
                        .municipality(shopOwner.getMunicipality())
                        .ward(shopOwner.getWard())
                        .tole(shopOwner.getTole())
                        // Shop Details
                        .shopType(shopOwner.getShopType())
                        .companyName(shopOwner.getCompanyName())
                        .shopPhone(shopOwner.getShopPhone())
                        .shopEmail(shopOwner.getShopEmail())
                        .shopLogo(shopOwner.getShopLogo())
                        .pan(shopOwner.getPan())
                        .regCert(shopOwner.getRegCert())
                        .vat(shopOwner.getVat())
                        .openingTime(shopOwner.getOpeningTime())
                        .closingTime(shopOwner.getClosingTime())
                        .offDays(shopOwner.getOffDays())
                        // Subscription Details
                        .subscriptionPlan(shopOwner.getSubscriptionPlan())
                        .subscriptionStartDate(shopOwner.getSubscriptionStartDate())
                        .subscriptionExpiryDate(shopOwner.getSubscriptionExpiryDate())
                        .vehicleLimit(shopOwner.getVehicleLimit())
                        .staffLimit(shopOwner.getStaffLimit())
                        // Important Data & Links
                        .citizenshipUpload(shopOwner.getCitizenshipUpload())
                        .shopRegUpload(shopOwner.getShopRegUpload())
                        .whatsappNo(shopOwner.getWhatsappNo())
                        .facebookPage(shopOwner.getFacebookPage())
                        .googleMapLink(shopOwner.getGoogleMapLink())
                        .notes(shopOwner.getNotes())
                        // Additional fields
                        .city(shopOwner.getCity())
                        .state(shopOwner.getState())
                        .postalCode(shopOwner.getPostalCode())
                        .country(shopOwner.getCountry())
                        .website(shopOwner.getWebsite())
                        .description(shopOwner.getDescription())
                        .build();
                
                // Apply filters
                if ((status == null || status.equals(credential.getStatus())) &&
                    (search == null || search.isEmpty() || 
                     credential.getEmail().toLowerCase().contains(search.toLowerCase()) ||
                     credential.getName().toLowerCase().contains(search.toLowerCase()) ||
                     (credential.getShopName() != null && credential.getShopName().toLowerCase().contains(search.toLowerCase())))) {
                    allCredentials.add(credential);
                }
            }
        });
        
        // Add public users
        allUsers.forEach(user -> {
            if (user.getRole() == UserRole.PUBLIC && (userType == null || userType.equals("PUBLIC_USER"))) {
                CredentialsDto credential = CredentialsDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .password("********") // Masked password
                        .role("PUBLIC_USER")
                        .userType("PUBLIC_USER")
                        .status(user.isActive() ? "ACTIVE" : "INACTIVE")
                        .createdAt(user.getCreatedAt())
                        .name(user.getFirstName() + " " + user.getLastName())
                        .phone(user.getPhone())
                        .shopName(null)
                        .isEmailVerified(user.isEmailVerified())
                        .build();
                
                // Apply filters
                if ((status == null || status.equals(credential.getStatus())) &&
                    (search == null || search.isEmpty() || 
                     credential.getEmail().toLowerCase().contains(search.toLowerCase()) ||
                     credential.getName().toLowerCase().contains(search.toLowerCase()))) {
                    allCredentials.add(credential);
                }
            }
        });
        
        // Sort allCredentials by createdAt descending (most recent first)
        allCredentials.sort((c1, c2) -> {
            if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
            if (c1.getCreatedAt() == null) return 1;
            if (c2.getCreatedAt() == null) return -1;
            return c2.getCreatedAt().compareTo(c1.getCreatedAt());
        });
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allCredentials.size());
        java.util.List<CredentialsDto> pageContent = allCredentials.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allCredentials.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialsDto getCredentialById(Long id) {
        // Try to find in shop owners first
        if (shopOwnerRepository.existsById(id)) {
            ShopOwner shopOwner = shopOwnerRepository.findById(id).orElse(null);
            return CredentialsDto.builder()
                    .id(shopOwner.getId())
                    .email(shopOwner.getEmail())
                    .password("********")
                    .role("SHOP_OWNER")
                    .userType("SHOP_OWNER")
                    .status(shopOwner.isActive() ? "ACTIVE" : "INACTIVE")
                    .approvalStatus(shopOwner.getApprovalStatus() != null ? shopOwner.getApprovalStatus() : "APPROVED")
                    .rejectionReason(shopOwner.getRejectionReason())
                    .createdAt(shopOwner.getCreatedAt())
                    .name(shopOwner.getFirstName() + " " + shopOwner.getLastName())
                    .phone(shopOwner.getPhone())
                    .shopName(shopOwner.getCompanyName())
                    .isEmailVerified(true)
                    // Owner Information
                    .firstName(shopOwner.getFirstName())
                    .lastName(shopOwner.getLastName())
                    .fatherName(shopOwner.getFatherName())
                    .address(shopOwner.getAddress())
                    .citizenshipNo(shopOwner.getCitizenshipNo())
                    .profilePhoto(shopOwner.getProfilePhoto())
                    .citizenshipPicFront(shopOwner.getCitizenshipPicFront())
                    .citizenshipPicBack(shopOwner.getCitizenshipPicBack())
                    // Location fields
                    .province(shopOwner.getProvince())
                    .district(shopOwner.getDistrict())
                    .municipality(shopOwner.getMunicipality())
                    .ward(shopOwner.getWard())
                    .tole(shopOwner.getTole())
                    // Shop Details
                    .shopType(shopOwner.getShopType())
                    .companyName(shopOwner.getCompanyName())
                    .shopPhone(shopOwner.getShopPhone())
                    .shopEmail(shopOwner.getShopEmail())
                    .shopLogo(shopOwner.getShopLogo())
                    .pan(shopOwner.getPan())
                    .regCert(shopOwner.getRegCert())
                    .vat(shopOwner.getVat())
                    .openingTime(shopOwner.getOpeningTime())
                    .closingTime(shopOwner.getClosingTime())
                    .offDays(shopOwner.getOffDays())
                    // Subscription Details
                    .subscriptionPlan(shopOwner.getSubscriptionPlan())
                    .subscriptionStartDate(shopOwner.getSubscriptionStartDate())
                    .subscriptionExpiryDate(shopOwner.getSubscriptionExpiryDate())
                    .vehicleLimit(shopOwner.getVehicleLimit())
                    .staffLimit(shopOwner.getStaffLimit())
                    // Important Data & Links
                    .citizenshipUpload(shopOwner.getCitizenshipUpload())
                    .shopRegUpload(shopOwner.getShopRegUpload())
                    .whatsappNo(shopOwner.getWhatsappNo())
                    .facebookPage(shopOwner.getFacebookPage())
                    .googleMapLink(shopOwner.getGoogleMapLink())
                    .notes(shopOwner.getNotes())
                    // Additional fields
                    .city(shopOwner.getCity())
                    .state(shopOwner.getState())
                    .postalCode(shopOwner.getPostalCode())
                    .country(shopOwner.getCountry())
                    .website(shopOwner.getWebsite())
                    .description(shopOwner.getDescription())
                    .build();
        }
        
        // Try to find in users
        if (userRepository.existsById(id)) {
            User user = userRepository.findById(id).orElse(null);
            return CredentialsDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .password("********")
                    .role("PUBLIC_USER")
                    .userType("PUBLIC_USER")
                    .status(user.isActive() ? "ACTIVE" : "INACTIVE")
                    .createdAt(user.getCreatedAt())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .phone(user.getPhone())
                    .isEmailVerified(user.isEmailVerified())
                    .build();
        }
        
        throw new RuntimeException("Credential not found with id: " + id);
    }

    @Override
    public String resetUserPassword(Long id) {
        String newPassword = new PasswordGeneratorUtil().generateTemporaryPassword(12);
        
        // Try to find and update shop owner
        if (shopOwnerRepository.existsById(id)) {
            ShopOwner shopOwner = shopOwnerRepository.findById(id).orElse(null);
            shopOwner.setPassword(newPassword); // Assuming password field exists and will be encoded
            shopOwnerRepository.save(shopOwner);
            return newPassword;
        }
        
        // Try to find and update user
        if (userRepository.existsById(id)) {
            User user = userRepository.findById(id).orElse(null);
            user.setPassword(newPassword); // Assuming password field exists and will be encoded
            userRepository.save(user);
            return newPassword;
        }
        
        throw new RuntimeException("User not found with id: " + id);
    }

    @Override
    public void toggleCredentialStatus(Long id) {
        // Try to find and update shop owner
        if (shopOwnerRepository.existsById(id)) {
            ShopOwner shopOwner = shopOwnerRepository.findById(id).orElse(null);
            shopOwner.setActive(!shopOwner.isActive());
            shopOwnerRepository.save(shopOwner);
            return;
        }
        
        // Try to find and update user
        if (userRepository.existsById(id)) {
            User user = userRepository.findById(id).orElse(null);
            user.setActive(!user.isActive());
            userRepository.save(user);
            return;
        }
        
        throw new RuntimeException("User not found with id: " + id);
    }

    @Override
    public void deleteCredential(Long id) {
        // Try to delete shop owner
        if (shopOwnerRepository.existsById(id)) {
            shopOwnerRepository.deleteById(id);
            return;
        }
        
        // Try to delete user
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return;
        }
        
        throw new RuntimeException("User not found with id: " + id);
    }
}
