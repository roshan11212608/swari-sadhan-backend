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
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.shop.entity.ShopReview;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.dashboard.dto.DashboardStatsDto;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.dashboard.dto.UserManagementDto;
import swari.sewa.module.dashboard.dto.CredentialsDto;
import swari.sewa.module.dashboard.dto.ShopDashboardSummaryDto;
import swari.sewa.module.dashboard.service.DashboardService;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.common.util.PasswordGeneratorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ShopReviewRepository shopReviewRepository;
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

    // ── ShopOwner Dashboard Summary ──
    // Replaces 3 heavy frontend API calls (500 vehicles, 20 enquiries, all reviews)
    // with a single lightweight response using count queries + 5-item paginated lists.
    @Override
    @Transactional(readOnly = true)
    public ShopDashboardSummaryDto getShopDashboardSummary(Long shopId) {
        // Vehicle counts — 1 count query instead of loading 500 full VehicleDtos
        long available = vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.ACTIVE);
        // "published" in the frontend checks status === 'PUBLISHED' || published === true.
        // PUBLISHED does not exist in VehicleStatus enum and Vehicle has no published field,
        // so this is always 0. Preserved for frontend compatibility.
        long published = 0;

        // Enquiry counts — 1 count query instead of loading 20 EnquiryDtos
        Long pendingEnquiries = enquiryRepository.countByShopIdAndStatus(shopId, EnquiryStatus.PENDING);
        long pending = pendingEnquiries != null ? pendingEnquiries : 0;

        // Review summary — 2 lightweight queries instead of loading ALL reviews
        long reviewCount = shopReviewRepository.countByShopId(shopId);
        double avgRating = shopReviewRepository.getAverageRatingByShopId(shopId);

        // Recent 5 reviews — paginated query instead of loading all + slicing in JS
        PageRequest recentReviewsPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ShopReview> recentReviewEntities = shopReviewRepository
                .findByShopIdOrderByCreatedAtDesc(shopId, recentReviewsPageable)
                .getContent();
        List<ShopDashboardSummaryDto.RecentReview> recentReviews = recentReviewEntities.stream()
                .map(r -> ShopDashboardSummaryDto.RecentReview.builder()
                        .id(r.getId())
                        .reviewerName(r.getReviewerName())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Recent 5 vehicles — paginated JOIN FETCH query instead of loading 500
        PageRequest recentVehiclesPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Vehicle> recentVehicleEntities = vehicleRepository
                .findByShopIdWithDetails(shopId, recentVehiclesPageable)
                .getContent();
        List<ShopDashboardSummaryDto.RecentVehicle> recentVehicles = recentVehicleEntities.stream()
                .map(v -> ShopDashboardSummaryDto.RecentVehicle.builder()
                        .id(v.getId())
                        .title(v.getTitle())
                        .brand(v.getBrandName())
                        .model(v.getModelName())
                        .vehicleType(v.getVehicleType() != null ? v.getVehicleType().name() : null)
                        .sellPrice(v.getSellingPrice() != null ? v.getSellingPrice() : v.getPrice())
                        .mainImageUrl(v.getMainImageUrl())
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .sold(v.getStatus() == VehicleStatus.SOLD)
                        .createdAt(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Recent 5 enquiries — paginated JOIN FETCH query instead of loading 20
        PageRequest recentEnquiriesPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Enquiry> recentEnquiryEntities = enquiryRepository
                .findByShopIdWithCustomerVehicleShop(shopId, recentEnquiriesPageable)
                .getContent();
        List<ShopDashboardSummaryDto.RecentEnquiry> recentEnquiries = recentEnquiryEntities.stream()
                .map(e -> ShopDashboardSummaryDto.RecentEnquiry.builder()
                        .id(e.getId())
                        .status(e.getStatus() != null ? e.getStatus().name() : null)
                        .customerName(e.getCustomerName())
                        .vehicleTitle(e.getVehicle() != null ? e.getVehicle().getTitle() : null)
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ShopDashboardSummaryDto.builder()
                .vehicleCounts(ShopDashboardSummaryDto.VehicleCounts.builder()
                        .available(available)
                        .published(published)
                        .build())
                .enquiryCounts(ShopDashboardSummaryDto.EnquiryCounts.builder()
                        .pending(pending)
                        .build())
                .reviewSummary(ShopDashboardSummaryDto.ReviewSummary.builder()
                        .count(reviewCount)
                        .averageRating(avgRating)
                        .recentReviews(recentReviews)
                        .build())
                .recentVehicles(recentVehicles)
                .recentEnquiries(recentEnquiries)
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
        return enquiryRepository.findAllWithCustomerVehicleShop(pageable)
                .map(enquiry -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("id", enquiry.getId());
                    report.put("customerName", enquiry.getCustomer() != null
                            ? enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName()
                            : enquiry.getCustomerName());
                    report.put("vehicleTitle", enquiry.getVehicle() != null ? enquiry.getVehicle().getTitle() : null);
                    report.put("status", enquiry.getStatus());
                    report.put("createdAt", enquiry.getCreatedAt());
                    return report;
                });
    }

    // Credentials Management Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public Page<CredentialsDto> getCredentials(Pageable pageable, String userType, String status, String search) {
        Boolean activeStatus = null;
        if (status != null && !status.isEmpty()) {
            activeStatus = "ACTIVE".equalsIgnoreCase(status);
        }

        String effectiveSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // When userType is specified, query only the relevant table with DB-level
        // filtering and pagination — eliminates loading ALL users + ALL shop owners
        // into memory.
        if (userType != null) {
            switch (userType) {
                case "SHOP_OWNER":
                    return getShopOwnerCredentials(pageable, activeStatus, effectiveSearch);
                case "SUPERADMIN":
                    return getUserCredentials(pageable, UserRole.SUPERADMIN, activeStatus, effectiveSearch);
                case "PUBLIC_USER":
                    return getUserCredentials(pageable, UserRole.PUBLIC, activeStatus, effectiveSearch);
                default:
                    return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
            }
        }

        // When userType is null (all types), query each table separately with
        // pagination and merge results. This loads at most `size` items from each
        // table instead of ALL items.
        java.util.List<CredentialsDto> allCredentials = new java.util.ArrayList<>();

        // Query superadmins
        Page<User> superadminPage = queryUsersPage(pageable, UserRole.SUPERADMIN, activeStatus, effectiveSearch);
        superadminPage.forEach(user -> allCredentials.add(buildUserCredentialDto(user, "SUPERADMIN")));

        // Query shop owners
        Page<ShopOwner> shopOwnerPage = queryShopOwnersPage(pageable, activeStatus, effectiveSearch);
        shopOwnerPage.forEach(shopOwner -> allCredentials.add(buildShopOwnerCredentialDto(shopOwner)));

        // Query public users
        Page<User> publicUserPage = queryUsersPage(pageable, UserRole.PUBLIC, activeStatus, effectiveSearch);
        publicUserPage.forEach(user -> allCredentials.add(buildUserCredentialDto(user, "PUBLIC_USER")));

        // Sort by createdAt descending
        allCredentials.sort((c1, c2) -> {
            if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
            if (c1.getCreatedAt() == null) return 1;
            if (c2.getCreatedAt() == null) return -1;
            return c2.getCreatedAt().compareTo(c1.getCreatedAt());
        });

        // Apply in-memory pagination on the merged results
        long total = superadminPage.getTotalElements() + shopOwnerPage.getTotalElements() + publicUserPage.getTotalElements();
        int start = (int) Math.min(pageable.getOffset(), allCredentials.size());
        int end = Math.min((start + pageable.getPageSize()), allCredentials.size());
        java.util.List<CredentialsDto> pageContent = allCredentials.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, total);
    }

    private Page<CredentialsDto> getShopOwnerCredentials(Pageable pageable, Boolean activeStatus, String search) {
        Page<ShopOwner> shopOwnerPage = queryShopOwnersPage(pageable, activeStatus, search);
        return shopOwnerPage.map(this::buildShopOwnerCredentialDto);
    }

    private Page<CredentialsDto> getUserCredentials(Pageable pageable, UserRole role, Boolean activeStatus, String search) {
        Page<User> userPage = queryUsersPage(pageable, role, activeStatus, search);
        return userPage.map(user -> buildUserCredentialDto(user, role == UserRole.SUPERADMIN ? "SUPERADMIN" : "PUBLIC_USER"));
    }

    private Page<ShopOwner> queryShopOwnersPage(Pageable pageable, Boolean activeStatus, String search) {
        if (search != null && activeStatus != null) {
            return shopOwnerRepository.searchByKeywordAndActive(search, activeStatus, pageable);
        } else if (search != null) {
            return shopOwnerRepository.searchByKeyword(search, pageable);
        } else if (activeStatus != null) {
            return shopOwnerRepository.findByActive(activeStatus, pageable);
        } else {
            return shopOwnerRepository.findAll(pageable);
        }
    }

    private Page<User> queryUsersPage(Pageable pageable, UserRole role, Boolean activeStatus, String search) {
        if (search != null && activeStatus != null) {
            return userRepository.findByRoleAndSearchAndActive(role, search, activeStatus, pageable);
        } else if (search != null) {
            return userRepository.findByRoleAndSearch(role, search, pageable);
        } else if (activeStatus != null) {
            return userRepository.findByRoleAndActive(role, activeStatus, pageable);
        } else {
            return userRepository.findByRole(role, pageable);
        }
    }

    private CredentialsDto buildUserCredentialDto(User user, String role) {
        return CredentialsDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password("********")
                .role(role)
                .userType(role)
                .status(user.isActive() ? "ACTIVE" : "INACTIVE")
                .createdAt(user.getCreatedAt())
                .name(user.getFirstName() + " " + user.getLastName())
                .phone(user.getPhone())
                .shopName(null)
                .isEmailVerified(user.isEmailVerified())
                .build();
    }

    private CredentialsDto buildShopOwnerCredentialDto(ShopOwner shopOwner) {
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

    @Override
    @Transactional(readOnly = true)
    public CredentialsDto getCredentialById(Long id) {
        // Try to find in shop owners first — single query instead of existsById + findById
        Optional<ShopOwner> shopOwnerOpt = shopOwnerRepository.findById(id);
        if (shopOwnerOpt.isPresent()) {
            return buildShopOwnerCredentialDto(shopOwnerOpt.get());
        }

        // Try to find in users — single query instead of existsById + findById
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String role = user.getRole() == UserRole.SUPERADMIN ? "SUPERADMIN" : "PUBLIC_USER";
            return buildUserCredentialDto(user, role);
        }

        throw new RuntimeException("Credential not found with id: " + id);
    }

    @Override
    public String resetUserPassword(Long id) {
        String newPassword = new PasswordGeneratorUtil().generateTemporaryPassword(12);

        // Try to find and update shop owner — single query instead of existsById + findById
        Optional<ShopOwner> shopOwnerOpt = shopOwnerRepository.findById(id);
        if (shopOwnerOpt.isPresent()) {
            ShopOwner shopOwner = shopOwnerOpt.get();
            shopOwner.setPassword(newPassword);
            shopOwnerRepository.save(shopOwner);
            return newPassword;
        }

        // Try to find and update user — single query instead of existsById + findById
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword);
            userRepository.save(user);
            return newPassword;
        }

        throw new RuntimeException("User not found with id: " + id);
    }

    @Override
    public void toggleCredentialStatus(Long id) {
        // Try to find and update shop owner — single query instead of existsById + findById
        Optional<ShopOwner> shopOwnerOpt = shopOwnerRepository.findById(id);
        if (shopOwnerOpt.isPresent()) {
            ShopOwner shopOwner = shopOwnerOpt.get();
            shopOwner.setActive(!shopOwner.isActive());
            shopOwnerRepository.save(shopOwner);
            return;
        }

        // Try to find and update user — single query instead of existsById + findById
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
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
