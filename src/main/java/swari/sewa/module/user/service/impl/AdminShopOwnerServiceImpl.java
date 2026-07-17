package swari.sewa.module.user.service.impl;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.user.service.AdminShopOwnerService;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminShopOwnerServiceImpl implements AdminShopOwnerService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

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
        // Handle owner name - split into first and last name if needed
        String firstName = shopOwnerDto.getFirstName();
        String lastName = shopOwnerDto.getLastName();
        
        if (shopOwnerDto.getOwnerName() != null && !shopOwnerDto.getOwnerName().trim().isEmpty()) {
            String[] nameParts = shopOwnerDto.getOwnerName().split(" ", 2);
            firstName = nameParts[0];
            lastName = nameParts.length > 1 ? nameParts[1] : "";
        }
        
        // Create ShopOwner with all fields
        ShopOwner shopOwner = ShopOwner.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(shopOwnerDto.getEmail())
                .password(passwordEncoder.encode(shopOwnerDto.getPassword()))
                .phone(shopOwnerDto.getPhone())
                .companyName(shopOwnerDto.getCompanyName() != null ? shopOwnerDto.getCompanyName() : shopOwnerDto.getShopName())
                .fatherName(shopOwnerDto.getFatherName())
                .address(shopOwnerDto.getAddress())
                .profilePhoto(shopOwnerDto.getProfilePhoto())
                .citizenshipNo(shopOwnerDto.getCitizenshipNo())
                .citizenshipPicFront(shopOwnerDto.getCitizenshipPicFront())
                .citizenshipPicBack(shopOwnerDto.getCitizenshipPicBack())
                .shopName(shopOwnerDto.getShopName())
                .shopType(shopOwnerDto.getShopType())
                .province(shopOwnerDto.getProvince())
                .district(shopOwnerDto.getDistrict())
                .municipality(shopOwnerDto.getMunicipality())
                .ward(shopOwnerDto.getWard())
                .tole(shopOwnerDto.getTole())
                .shopPhone(shopOwnerDto.getShopPhone())
                .shopEmail(shopOwnerDto.getShopEmail())
                .shopLogo(shopOwnerDto.getShopLogo())
                .pan(shopOwnerDto.getPan())
                .regCert(shopOwnerDto.getRegCert())
                .vat(shopOwnerDto.getVat())
                .openingTime(shopOwnerDto.getOpeningTime())
                .closingTime(shopOwnerDto.getClosingTime())
                .offDays(shopOwnerDto.getOffDays())
                .subscriptionPlan(shopOwnerDto.getPlan())
                .subscriptionStartDate(shopOwnerDto.getStartDate())
                .subscriptionExpiryDate(shopOwnerDto.getExpiryDate())
                .vehicleLimit(shopOwnerDto.getVehicleLimit() != null ? shopOwnerDto.getVehicleLimit() : Integer.valueOf(5))
                .staffLimit(shopOwnerDto.getStaffLimit() != null ? shopOwnerDto.getStaffLimit() : Integer.valueOf(3))
                .citizenshipUpload(shopOwnerDto.getCitizenshipUpload())
                .shopRegUpload(shopOwnerDto.getShopRegUpload())
                .whatsappNo(shopOwnerDto.getWhatsappNo())
                .facebookPage(shopOwnerDto.getFacebookPage())
                .googleMapLink(shopOwnerDto.getGoogleMapLink())
                .notes(shopOwnerDto.getNotes())
                .active(shopOwnerDto.getActive() != null ? shopOwnerDto.getActive() : true)
                .role(UserRole.SHOP_OWNER)
                .emailVerified(true)
                .subscriptionActive(true)
                .build();
        
        ShopOwner savedShopOwner = shopOwnerRepository.save(shopOwner);

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
                profilePhotoUrl = fileStorageService.storeFile(profilePhoto);
            }
            if (shopLogo != null && !shopLogo.isEmpty()) {
                shopLogoUrl = fileStorageService.storeFile(shopLogo);
            }
            if (citizenshipPicFront != null && !citizenshipPicFront.isEmpty()) {
                citizenshipPicFrontUrl = fileStorageService.storeFile(citizenshipPicFront);
            }
            if (citizenshipPicBack != null && !citizenshipPicBack.isEmpty()) {
                citizenshipPicBackUrl = fileStorageService.storeFile(citizenshipPicBack);
            }
            if (shopRegUpload != null && !shopRegUpload.isEmpty()) {
                shopRegUploadUrl = fileStorageService.storeFile(shopRegUpload);
            }

            shopOwnerDto.setProfilePhoto(profilePhotoUrl);
            shopOwnerDto.setShopLogo(shopLogoUrl);
            shopOwnerDto.setCitizenshipPicFront(citizenshipPicFrontUrl);
            shopOwnerDto.setCitizenshipPicBack(citizenshipPicBackUrl);
            shopOwnerDto.setShopRegUpload(shopRegUploadUrl);

            return createShopOwner(shopOwnerDto);
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
    public void deleteShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        
        // Delete ShopOwner directly
        shopOwnerRepository.delete(shopOwner);
    }

    @Override
    public void approveShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void rejectShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void suspendShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void reactivateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
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
                    vehicleData.put("price", vehicle.getPrice());
                    vehicleData.put("views", vehicle.getViewCount());
                    vehicleData.put("createdAt", vehicle.getCreatedAt());
                    return vehicleData;
                });
    }
}
