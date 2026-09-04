package swari.sewa.module.user.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.service.ImageType;
import swari.sewa.common.service.StorageCategory;
import swari.sewa.common.service.StorageService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.auth.entity.ShopRegOtp;
import swari.sewa.module.auth.repository.ShopRegOtpRepository;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.user.service.AdminShopOwnerService;
import swari.sewa.module.subscription.service.TrialSubscriptionService;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminShopOwnerServiceImpl implements AdminShopOwnerService {

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final ShopRegOtpRepository shopRegOtpRepository;
    private final EmailService emailService;
    private final TrialSubscriptionService trialSubscriptionService;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOwnerDto> getAllShopOwners(Pageable pageable, String search, String status) {
        Page<ShopOwner> shopOwners;
        
        if (search != null && !search.trim().isEmpty()) {
            shopOwners = shopOwnerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            boolean isActive = "active".equalsIgnoreCase(status);
            shopOwners = shopOwnerRepository.findByActive(isActive, pageable);
        } else {
            shopOwners = shopOwnerRepository.findAll(pageable);
        }
        
        return shopOwners.map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopOwnerDto getShopOwnerById(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        
        return convertToDto(shopOwner);
    }

    @Override
    public ShopOwnerDto createShopOwner(ShopOwnerDto shopOwnerDto) {
        // Validate the OTP verification token
        String token = shopOwnerDto.getSignupVerificationToken();
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Verification token is required. Please verify your email first.");
        }

        String email = shopOwnerDto.getEmail().trim().toLowerCase();
        String mobile = normalizeMobile(shopOwnerDto.getPhone());

        ShopRegOtp otpRecord = shopRegOtpRepository.findLatestByEmailAndMobile(email, mobile)
                .or(() -> shopRegOtpRepository.findLatestByEmail(email))
                .orElseThrow(() -> new RuntimeException("No verification found. Please verify your email first."));

        if (!otpRecord.isVerified() || otpRecord.getUsedAt() != null) {
            throw new RuntimeException("Verification is invalid or already used. Please start over.");
        }

        if (otpRecord.getTokenExpiresAt() == null
                || otpRecord.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification has expired. Please request new codes.");
        }

        String tokenHash = hashSha256(token);
        if (!tokenHash.equals(otpRecord.getVerificationTokenHash())) {
            throw new RuntimeException("Invalid verification token. Please verify your email again.");
        }

        // Consume the token so it can't be reused
        otpRecord.setUsedAt(LocalDateTime.now());
        shopRegOtpRepository.save(otpRecord);

        // Handle owner name - split into first and last name if needed
        String firstName = shopOwnerDto.getFirstName();
        String lastName = shopOwnerDto.getLastName();

        if (shopOwnerDto.getOwnerName() != null && !shopOwnerDto.getOwnerName().trim().isEmpty()) {
            String[] nameParts = shopOwnerDto.getOwnerName().split(" ", 2);
            firstName = nameParts[0];
            lastName = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Check for an existing shop_owners record with this email. Only
        // REJECTED applications may be re-submitted; the existing row is reused
        // so no duplicate is created. PENDING and APPROVED rows are blocked.
        Optional<ShopOwner> existingOpt = shopOwnerRepository.findByEmail(email);
        ShopOwner shopOwner;
        boolean isReapplication = false;

        if (existingOpt.isPresent()) {
            ShopOwner existing = existingOpt.get();
            String status = existing.getApprovalStatus();
            if ("PENDING".equals(status)) {
                throw new RuntimeException("Your application is already pending admin approval.");
            }
            if ("APPROVED".equals(status)) {
                throw new RuntimeException("An account with this email already exists. Kindly login.");
            }
            // REJECTED → reuse the existing record
            shopOwner = existing;
            isReapplication = true;
        } else {
            shopOwner = new ShopOwner();
        }

        // Apply registration fields (shared by new and re-application paths)
        shopOwner.setFirstName(firstName);
        shopOwner.setLastName(lastName);
        shopOwner.setEmail(email);
        shopOwner.setPhone(mobile);
        shopOwner.setCompanyName(shopOwnerDto.getCompanyName() != null ? shopOwnerDto.getCompanyName() : shopOwnerDto.getShopName());
        shopOwner.setFatherName(shopOwnerDto.getFatherName());
        shopOwner.setAddress(shopOwnerDto.getAddress());
        shopOwner.setProfilePhoto(shopOwnerDto.getProfilePhoto());
        shopOwner.setCitizenshipNo(shopOwnerDto.getCitizenshipNo());
        shopOwner.setCitizenshipPicFront(shopOwnerDto.getCitizenshipPicFront());
        shopOwner.setCitizenshipPicBack(shopOwnerDto.getCitizenshipPicBack());
        shopOwner.setShopName(shopOwnerDto.getShopName());
        shopOwner.setShopType(shopOwnerDto.getShopType());
        shopOwner.setProvince(shopOwnerDto.getProvince());
        shopOwner.setDistrict(shopOwnerDto.getDistrict());
        shopOwner.setMunicipality(shopOwnerDto.getMunicipality());
        shopOwner.setWard(shopOwnerDto.getWard());
        shopOwner.setTole(shopOwnerDto.getTole());
        shopOwner.setShopPhone(shopOwnerDto.getShopPhone());
        shopOwner.setShopEmail(shopOwnerDto.getShopEmail());
        shopOwner.setShopLogo(shopOwnerDto.getShopLogo());
        shopOwner.setPan(shopOwnerDto.getPan());
        shopOwner.setRegCert(shopOwnerDto.getRegCert());
        shopOwner.setVat(shopOwnerDto.getVat());
        shopOwner.setOpeningTime(shopOwnerDto.getOpeningTime());
        shopOwner.setClosingTime(shopOwnerDto.getClosingTime());
        shopOwner.setOffDays(shopOwnerDto.getOffDays());
        shopOwner.setSubscriptionPlan(shopOwnerDto.getPlan());
        shopOwner.setSubscriptionStartDate(shopOwnerDto.getStartDate());
        shopOwner.setSubscriptionExpiryDate(shopOwnerDto.getExpiryDate());
        shopOwner.setVehicleLimit(shopOwnerDto.getVehicleLimit() != null ? shopOwnerDto.getVehicleLimit() : Integer.valueOf(5));
        shopOwner.setStaffLimit(shopOwnerDto.getStaffLimit() != null ? shopOwnerDto.getStaffLimit() : Integer.valueOf(3));
        shopOwner.setCitizenshipUpload(shopOwnerDto.getCitizenshipUpload());
        shopOwner.setShopRegUpload(shopOwnerDto.getShopRegUpload());
        shopOwner.setWhatsappNo(shopOwnerDto.getWhatsappNo());
        shopOwner.setFacebookPage(shopOwnerDto.getFacebookPage());
        shopOwner.setGoogleMapLink(shopOwnerDto.getGoogleMapLink());
        shopOwner.setNotes(shopOwnerDto.getNotes());

        // Application state — identical for a fresh application and a re-application.
        // Password is a random placeholder; the real temp password is generated on
        // approval and emailed to the user.
        shopOwner.setActive(false);
        shopOwner.setRole(UserRole.SHOP_OWNER);
        shopOwner.setEmailVerified(true);
        shopOwner.setSubscriptionActive(false);
        shopOwner.setApprovalStatus("PENDING");
        shopOwner.setPasswordChanged(false);
        shopOwner.setPassword(passwordEncoder.encode(generateRandomPassword(16)));

        // Clear stale rejection data so the re-applied application starts clean.
        shopOwner.setRejectionReason(null);
        shopOwner.setApprovedAt(null);

        ShopOwner savedShopOwner = shopOwnerRepository.save(shopOwner);

        // On re-application, deactivate the mirrored users row (if one exists
        // from a previous approval cycle) so it cannot be used to log in until
        // the new application is approved.
        if (isReapplication) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setActive(false);
                userRepository.save(user);
            });
        }

        return convertToDto(savedShopOwner);
    }

    @Override
    public ShopOwnerDto createShopOwnerWithFiles(String shopOwnerDataJson, MultipartFile profilePhoto, MultipartFile shopLogo, MultipartFile citizenshipPicFront, MultipartFile citizenshipPicBack, MultipartFile shopRegUpload) {
        try {
            ShopOwnerDto shopOwnerDto = objectMapper.readValue(shopOwnerDataJson, ShopOwnerDto.class);

            String profilePhotoUrl = null;
            String shopLogoUrl = null;
            String citizenshipPicFrontUrl = null;
            String citizenshipPicBackUrl = null;
            String shopRegUploadUrl = null;

            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                profilePhotoUrl = storageService.store(profilePhoto, StorageCategory.USER, null, ImageType.PROFILE_PHOTO);
            }
            if (shopLogo != null && !shopLogo.isEmpty()) {
                shopLogoUrl = storageService.store(shopLogo, StorageCategory.SHOP, null, ImageType.SHOP_LOGO);
            }
            if (citizenshipPicFront != null && !citizenshipPicFront.isEmpty()) {
                citizenshipPicFrontUrl = storageService.store(citizenshipPicFront, StorageCategory.USER, null, ImageType.USER_DOCUMENT);
            }
            if (citizenshipPicBack != null && !citizenshipPicBack.isEmpty()) {
                citizenshipPicBackUrl = storageService.store(citizenshipPicBack, StorageCategory.USER, null, ImageType.USER_DOCUMENT);
            }
            if (shopRegUpload != null && !shopRegUpload.isEmpty()) {
                shopRegUploadUrl = storageService.store(shopRegUpload, StorageCategory.SHOP_REGISTRATION, null, ImageType.SHOP_REGISTRATION_DOC);
            }

            shopOwnerDto.setProfilePhoto(profilePhotoUrl);
            shopOwnerDto.setShopLogo(shopLogoUrl);
            shopOwnerDto.setCitizenshipPicFront(citizenshipPicFrontUrl);
            shopOwnerDto.setCitizenshipPicBack(citizenshipPicBackUrl);
            shopOwnerDto.setShopRegUpload(shopRegUploadUrl);

            return createShopOwner(shopOwnerDto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error processing shop owner registration with files", e);
        }
    }

    private ShopOwnerDto convertToDto(ShopOwner shopOwner) {
        return ShopOwnerDto.builder()
                .id(shopOwner.getId())
                .firstName(shopOwner.getFirstName())
                .lastName(shopOwner.getLastName())
                .fullName(shopOwner.getFirstName() + " " + shopOwner.getLastName())
                .ownerName(shopOwner.getFirstName() + " " + shopOwner.getLastName())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhone())
                .companyName(shopOwner.getCompanyName())
                .fatherName(shopOwner.getFatherName())
                .address(shopOwner.getAddress())
                .profilePhoto(shopOwner.getProfilePhoto())
                .citizenshipNo(shopOwner.getCitizenshipNo())
                .citizenshipPicFront(shopOwner.getCitizenshipPicFront())
                .citizenshipPicBack(shopOwner.getCitizenshipPicBack())
                .shopName(shopOwner.getShopName())
                .shopType(shopOwner.getShopType())
                .province(shopOwner.getProvince())
                .district(shopOwner.getDistrict())
                .municipality(shopOwner.getMunicipality())
                .ward(shopOwner.getWard())
                .tole(shopOwner.getTole())
                .shopPhone(shopOwner.getShopPhone())
                .shopEmail(shopOwner.getShopEmail())
                .shopLogo(shopOwner.getShopLogo())
                .pan(shopOwner.getPan())
                .regCert(shopOwner.getRegCert())
                .vat(shopOwner.getVat())
                .openingTime(shopOwner.getOpeningTime())
                .closingTime(shopOwner.getClosingTime())
                .offDays(shopOwner.getOffDays())
                .plan(shopOwner.getSubscriptionPlan())
                .startDate(shopOwner.getSubscriptionStartDate())
                .expiryDate(shopOwner.getSubscriptionExpiryDate())
                .vehicleLimit(shopOwner.getVehicleLimit())
                .staffLimit(shopOwner.getStaffLimit())
                .citizenshipUpload(shopOwner.getCitizenshipUpload())
                .shopRegUpload(shopOwner.getShopRegUpload())
                .whatsappNo(shopOwner.getWhatsappNo())
                .facebookPage(shopOwner.getFacebookPage())
                .googleMapLink(shopOwner.getGoogleMapLink())
                .notes(shopOwner.getNotes())
                .active(shopOwner.isActive())
                .createdAt(shopOwner.getCreatedAt())
                .approvalStatus(shopOwner.getApprovalStatus())
                .passwordChanged(shopOwner.getPasswordChanged())
                .rejectionReason(shopOwner.getRejectionReason())
                .approvedAt(shopOwner.getApprovedAt())
                .build();
    }

    @Override
    public ShopOwnerDto updateShopOwner(Long id, ShopOwnerDto shopOwnerDto) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        
        // Update ShopOwner fields
        shopOwner.setFirstName(shopOwnerDto.getFirstName());
        shopOwner.setLastName(shopOwnerDto.getLastName());
        shopOwner.setPhone(shopOwnerDto.getPhone());
        shopOwner.setCompanyName(shopOwnerDto.getCompanyName());
        
        ShopOwner savedShopOwner = shopOwnerRepository.save(shopOwner);
        
        return ShopOwnerDto.builder()
                .id(savedShopOwner.getId())
                .firstName(savedShopOwner.getFirstName())
                .lastName(savedShopOwner.getLastName())
                .email(savedShopOwner.getEmail())
                .phone(savedShopOwner.getPhone())
                .companyName(savedShopOwner.getCompanyName())
                .active(savedShopOwner.isActive())
                .createdAt(savedShopOwner.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        Long shopOwnerId = shopOwner.getId();
        String email = shopOwner.getEmail();

        log.info("Starting deletion of shop owner {} ({})", shopOwnerId, email);

        // Disable FK checks during deletion to avoid ordering issues.
        // TiDB / MySQL: SET FOREIGN_KEY_CHECKS = 0
        try {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        } catch (Exception ignored) {}

        try {
            // Delete all related records — order no longer matters with FK checks off,
            // but we still go child-first for cleanliness.

            // Vehicle-related
            safeExecuteUpdate("DELETE w FROM wishlists w INNER JOIN vehicles v ON w.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE e FROM enquiries e INNER JOIN vehicles v ON e.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE sa FROM sell_applications sa INNER JOIN vehicles v ON sa.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE sva FROM sell_vehicle_applications sva INNER JOIN vehicles v ON sva.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE vi FROM vehicle_images vi INNER JOIN vehicles v ON vi.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE pvl FROM public_vehicle_listings pvl INNER JOIN vehicles v ON pvl.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE pvlrh FROM public_vehicle_listing_review_history pvlrh INNER JOIN vehicles v ON pvlrh.vehicle_id = v.id INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE v FROM vehicles v INNER JOIN shops s ON v.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);

            // Employee-related
            safeExecuteUpdate("DELETE sr FROM salary_records sr INNER JOIN employees e ON sr.employee_id = e.id INNER JOIN shops s ON e.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE lr FROM leave_requests lr INNER JOIN employees e ON lr.employee_id = e.id INNER JOIN shops s ON e.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE ap FROM advance_payments ap INNER JOIN employees e ON ap.employee_id = e.id INNER JOIN shops s ON e.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE att FROM attendance att INNER JOIN employees e ON att.employee_id = e.id INNER JOIN shops s ON e.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE e FROM employees e INNER JOIN shops s ON e.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);

            // Shop-related
            safeExecuteUpdate("DELETE en FROM enquiries en INNER JOIN shops s ON en.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE ex FROM expenses ex INNER JOIN shops s ON ex.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE sr FROM shop_reviews sr INNER JOIN shops s ON sr.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE sva FROM sell_vehicle_applications sva INNER JOIN shops s ON sva.shop_id = s.id WHERE s.shop_owner_id = :id", shopOwnerId);

            // Subscriptions
            safeExecuteUpdate("DELETE st FROM subscription_transactions st INNER JOIN subscriptions sub ON st.subscription_id = sub.id WHERE sub.shop_owner_id = :id", shopOwnerId);
            safeExecuteUpdate("DELETE FROM subscriptions WHERE shop_owner_id = :id", shopOwnerId);

            // Shop owner permissions (table may not exist)
            safeExecuteUpdate("DELETE FROM shop_owner_permissions WHERE shop_owner_id = :id", shopOwnerId);

            // Shops
            safeExecuteUpdate("DELETE FROM shops WHERE shop_owner_id = :id", shopOwnerId);

            // OTP records
            safeExecuteUpdate("DELETE FROM shop_reg_otp WHERE email = :email", email);

            // Mirrored user row
            safeExecuteUpdate("DELETE FROM users WHERE email = :email", email);

            // The shop owner itself
            shopOwnerRepository.delete(shopOwner);
            shopOwnerRepository.flush();

            log.info("Successfully deleted shop owner {} ({}) and all related records", shopOwnerId, email);
        } finally {
            // Re-enable FK checks
            try {
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            } catch (Exception ignored) {}
        }
    }

    private void safeExecuteUpdate(String sql, Long id) {
        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", id);
            query.executeUpdate();
        } catch (Exception e) {
            log.warn("Skipping delete (table may not exist): {} — {}", sql.substring(0, Math.min(60, sql.length())), e.getMessage());
        }
    }

    private void safeExecuteUpdate(String sql, String email) {
        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("email", email);
            query.executeUpdate();
        } catch (Exception e) {
            log.warn("Skipping delete (table may not exist): {} — {}", sql.substring(0, Math.min(60, sql.length())), e.getMessage());
        }
    }

    private void executeUpdate(String sql, Long id) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", id);
        query.executeUpdate();
    }

    private void executeUpdate(String sql, String email) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("email", email);
        query.executeUpdate();
    }

    @Override
    public void approveShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        if ("APPROVED".equals(shopOwner.getApprovalStatus())) {
            throw new RuntimeException("This shop owner is already approved.");
        }

        // Generate a temporary password
        String tempPassword = generateRandomPassword(12);

        shopOwner.setActive(true);
        shopOwner.setApprovalStatus("APPROVED");
        shopOwner.setPasswordChanged(false);
        shopOwner.setPassword(passwordEncoder.encode(tempPassword));
        shopOwner.setSubscriptionActive(true);
        shopOwner.setApprovedAt(LocalDateTime.now());
        shopOwnerRepository.save(shopOwner);

        // Sync the temp password and active flag into the mirrored users row
        // (if one exists). UserDetailsServiceImpl reads users first, so without
        // this the owner would be authenticated against the stale placeholder
        // password from registration and login would fail with bad credentials.
        userRepository.findByEmail(shopOwner.getEmail()).ifPresent(user -> {
            user.setPassword(shopOwner.getPassword());
            user.setActive(true);
            userRepository.save(user);
        });

        // Send approval email with username + temp password
        String subject = "Swari Sadhan - Your Shop Registration is Approved!";
        String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#f97316'>Swari Sadhan</h2>"
                + "<p>Dear " + shopOwner.getFirstName() + ",</p>"
                + "<p>Congratulations! Your shop <strong>" + shopOwner.getShopName() + "</strong> has been approved.</p>"
                + "<p>You can now log in to your account using the credentials below:</p>"
                + "<div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin:16px 0'>"
                + "<p style='margin:4px 0'><strong>Username (Email):</strong> " + shopOwner.getEmail() + "</p>"
                + "<p style='margin:4px 0'><strong>Temporary Password:</strong> <code style='font-size:16px;background:#fff;padding:2px 8px;border-radius:4px'>" + tempPassword + "</code></p>"
                + "</div>"
                + "<p style='color:#dc2626;font-weight:600'>IMPORTANT: You must change your password on first login.</p>"
                + "<p>Click below to log in:</p>"
                + "<a href='" + frontendBaseUrl + "/login' style='display:inline-block;background:#f97316;color:white;padding:10px 24px;border-radius:6px;text-decoration:none;margin:8px 0'>Login to Swari Sadhan</a>"
                + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>If you did not register for Swari Sadhan, please ignore this email.</p>"
                + "</div>";
        emailService.sendHtmlEmail(shopOwner.getEmail(), subject, htmlBody);
    }

    @Override
    public void rejectShopOwner(Long id, String reason) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        shopOwner.setActive(false);
        shopOwner.setApprovalStatus("REJECTED");
        shopOwner.setRejectionReason(reason != null ? reason : "Your registration did not meet our requirements.");
        shopOwnerRepository.save(shopOwner);

        // Sync the mirrored users row (if one exists from a previous approval)
        // so the rejected owner cannot authenticate through it either.
        userRepository.findByEmail(shopOwner.getEmail()).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });

        // Send rejection email
        String subject = "Swari Sadhan - Shop Registration Update";
        String rejectionText = reason != null && !reason.isBlank() ? reason
                : "Your registration did not meet our requirements at this time.";
        String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#f97316'>Swari Sadhan</h2>"
                + "<p>Dear " + shopOwner.getFirstName() + ",</p>"
                + "<p>We have reviewed your shop registration for <strong>" + shopOwner.getShopName() + "</strong>.</p>"
                + "<p>Unfortunately, your registration could not be approved at this time.</p>"
                + "<div style='background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;margin:16px 0'>"
                + "<p style='margin:0'><strong>Reason:</strong> " + rejectionText + "</p>"
                + "</div>"
                + "<p>If you believe this is an error or would like to reapply, please contact our support team.</p>"
                + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>Swari Sadhan Team</p>"
                + "</div>";
        emailService.sendHtmlEmail(shopOwner.getEmail(), subject, htmlBody);
    }

    @Override
    @Transactional
    public void suspendShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);

        // Sync the active flag into the mirrored users row so login is blocked.
        // The login flow reads active from the users table, so without this
        // a suspended shop owner could still log in.
        userRepository.findByEmail(shopOwner.getEmail()).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });

        log.info("Suspended shop owner {} ({})", id, shopOwner.getEmail());
    }

    @Override
    @Transactional
    public void reactivateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);

        // Sync the active flag into the mirrored users row so login works again.
        userRepository.findByEmail(shopOwner.getEmail()).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
        });

        log.info("Reactivated shop owner {} ({})", id, shopOwner.getEmail());
    }

    @Override
    public void changeShopOwnerPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        // Detect first-time password change (temp password → real password)
        boolean wasFirstChange = !Boolean.TRUE.equals(shopOwner.getPasswordChanged());

        String encoded = passwordEncoder.encode(newPassword);
        shopOwner.setPassword(encoded);
        shopOwner.setPasswordChanged(true);
        shopOwnerRepository.save(shopOwner);
        syncMirroredUserPassword(shopOwner.getEmail(), encoded);

        // After the first password change (post-approval onboarding), auto-start trial.
        // Trial creation runs in its own transaction (REQUIRES_NEW) so that a trial
        // failure does NOT roll back the password change. The password is already
        // saved at this point. We log the failure but do not expose it to the user.
        if (wasFirstChange) {
            try {
                boolean started = trialSubscriptionService.startTrialIfNeeded(id);
                if (started) {
                    log.info("Trial subscription auto-started for shop owner {} after first password change", id);
                }
            } catch (Exception e) {
                log.error("Failed to auto-start trial subscription for shop owner {} after first password change: {}",
                        id, e.getMessage(), e);
                // Do NOT throw — the password change succeeded, and the trial
                // can be started manually later. Throwing would confuse the user
                // who just changed their password successfully.
            }
        }
    }

    /**
     * Shop owners can also have a row in the {@code users} table, created by
     * other flows to satisfy foreign keys. Login checks that table first, so a
     * password change must be written to both - otherwise the old temporary
     * password would still authenticate.
     */
    private void syncMirroredUserPassword(String email, String encodedPassword) {
        if (email == null) return;
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPassword(encodedPassword);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getShopOwnerShops(Long id, Pageable pageable) {
        return shopRepository.findByShopOwner_Id(id, pageable)
                .map(shop -> {
                    Map<String, Object> shopData = new HashMap<>();
                    shopData.put("id", shop.getId());
                    shopData.put("name", shop.getName());
                    shopData.put("status", shop.getStatus());
                    shopData.put("address", shop.getAddress());
                    shopData.put("createdAt", shop.getCreatedAt());
                    return shopData;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getShopOwnerVehicles(Long id, Pageable pageable) {
        return vehicleRepository.findByShop_ShopOwner_Id(id, pageable)
                .map(vehicle -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("id", vehicle.getId());
                    vehicleData.put("title", vehicle.getTitle());
                    vehicleData.put("status", vehicle.getStatus());
                    vehicleData.put("price", vehicle.getSellingPrice());
                    vehicleData.put("views", vehicle.getViewCount());
                    vehicleData.put("createdAt", vehicle.getCreatedAt());
                    return vehicleData;
                });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";

    static String normalizeMobile(String raw) {
        if (raw == null) throw new IllegalArgumentException("Mobile number is required");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("977")) digits = digits.substring(3);
        // Accept 10-digit Nepal mobile numbers starting with 9 (98, 97, 96, 95, 94, 92, etc.)
        if (digits.length() == 10 && digits.startsWith("9")) {
            return "+977" + digits;
        }
        throw new IllegalArgumentException(
                "Enter a valid Nepal mobile number (e.g. 98XXXXXXXX).");
    }

    private static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private static String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
