package swari.sewa.module.vehicle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import swari.sewa.module.vehicle.dto.VehicleDto;
import swari.sewa.common.dto.VehicleSearchRequest;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.vehicle.service.VehicleService;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.common.enums.ShopStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import swari.sewa.common.service.StorageCategory;
import swari.sewa.common.service.StorageService;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        maxAge = 3600
)
public class VehicleController {

    private final VehicleService vehicleService;
    private final ShopRepository shopRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final swari.sewa.module.subscription.service.SubscriptionAccessService subscriptionAccessService;

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : ("ROLE_" + role);
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (a != null && target.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long resolveCurrentShopIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        String email = authentication.getName();
        ShopOwner shopOwner = shopOwnerRepository.findByEmail(email).orElse(null);
        if (shopOwner == null || shopOwner.getId() == null) {
            return null;
        }
        return shopRepository.findByShopOwnerId(shopOwner.getId())
                .stream()
                .findFirst()
                .map(Shop::getId)
                .orElse(null);
    }

    /**
     * Resolve the authenticated shop owner's ID from the JWT context.
     */
    private Long resolveCurrentShopOwnerIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return shopOwnerRepository.findByEmail(authentication.getName())
                .map(ShopOwner::getId)
                .orElse(null);
    }

    // Simple test endpoint without database operations
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, String>> testConnection() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Backend connection is working");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current vehicle usage for the authenticated shop owner:
     * subscription status, plan name, current vehicle count, vehicle limit,
     * remaining slots, and trial information.
     */
    @GetMapping("/usage")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> getVehicleUsage() {
        Long shopOwnerId = resolveCurrentShopOwnerIdOrNull();
        if (shopOwnerId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Shop owner not found");
            error.put("message", "Could not resolve shop owner from authentication.");
            return ResponseEntity.status(403).body(error);
        }
        return ResponseEntity.ok(subscriptionAccessService.getVehicleUsage(shopOwnerId));
    }

    // Temporary endpoint to create a test shop for development
    @PostMapping("/create-test-shop")
    public ResponseEntity<Map<String, Object>> createTestShop() {
        try {
            // Create shop owner if not exists
            ShopOwner shopOwner = shopOwnerRepository.findByEmail("owner@shop.com")
                    .orElseGet(() -> {
                        ShopOwner newOwner = new ShopOwner();
                        newOwner.setFirstName("Shop");
                        newOwner.setLastName("Owner");
                        newOwner.setEmail("owner@shop.com");
                        newOwner.setPassword(passwordEncoder.encode("Owner@123"));
                        newOwner.setPhone("977-9841234568");
                        newOwner.setActive(true);
                        newOwner.setEmailVerified(true);
                        return newOwner;
                    });

            if (shopOwner.getId() == null) {
                shopOwnerRepository.save(shopOwner);
            }

            // Create shop if not exists
            if (!shopRepository.existsByLicenseNumber("SHOP-001")) {
                Shop shop = new Shop();
                shop.setName("Demo Bike Shop");
                shop.setDescription("A sample bike shop for testing vehicle creation");
                shop.setLicenseNumber("SHOP-001");
                shop.setAddressLine1("123 Main Street");
                shop.setCity("Kathmandu");
                shop.setState("Bagmati");
                shop.setCountry("Nepal");
                shop.setPostalCode("44600");
                shop.setPhoneNumber("977-9841234568");
                shop.setEmailAddress("demo@shop.com");
                shop.setStatus(ShopStatus.ACTIVE);
                shop.setIsFeatured(false);
                shop.setShopOwner(shopOwner);
                
                shopRepository.save(shop);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Test shop created successfully");
                response.put("shopId", shop.getId());
                return ResponseEntity.ok(response);
            } else {
                Shop existingShop = shopRepository.findByLicenseNumber("SHOP-001").orElse(null);
                Map<String, Object> response = new HashMap<>();
                response.put("status", "exists");
                response.put("message", "Test shop already exists");
                response.put("shopId", existingShop.getId());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error creating test shop: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    // @PreAuthorize("hasRole('SHOP_OWNER')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<?> createVehicle(@RequestPart(value = "vehicle", required = false) String vehicleJson,
                                          @RequestPart(value = "mediaFiles", required = false) MultipartFile[] mediaFiles,
                                          @RequestPart(value = "bluebookFiles", required = false) MultipartFile[] bluebookFiles,
                                          @RequestPart(value = "sellerPassportPhoto", required = false) MultipartFile sellerPassportPhoto,
                                          @RequestPart(value = "sellerCitizenshipFront", required = false) MultipartFile sellerCitizenshipFront,
                                          @RequestPart(value = "sellerCitizenshipBack", required = false) MultipartFile sellerCitizenshipBack,
                                          @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long shopIdToUse = shopId;

            // Prevent shop owners from spoofing shop id in headers.
            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                shopIdToUse = resolveCurrentShopIdOrNull();
            }

            // UI may send shopOwnerId as X-Shop-Id. If no shop exists with that ID, try resolving by shopOwnerId.
            if (shopIdToUse != null && !shopRepository.existsById(shopIdToUse)) {
                shopIdToUse = shopRepository.findByShopOwnerId(shopIdToUse)
                        .stream()
                        .findFirst()
                        .map(Shop::getId)
                        .orElse(null);
            }

            // If still not found, auto-create a shop for this shop owner (common in current data setup).
            if (shopIdToUse == null) {
                ShopOwner shopOwner = (shopId == null) ? null : shopOwnerRepository.findById(shopId).orElse(null);
                if (shopOwner != null) {
                    User user = userRepository.findByEmail(shopOwner.getEmail()).orElse(null);
                    if (user == null) {
                        user = userRepository.save(User.builder()
                                .email(shopOwner.getEmail())
                                .password(shopOwner.getPassword())
                                .firstName(shopOwner.getFirstName() != null ? shopOwner.getFirstName() : "Shop")
                                .lastName(shopOwner.getLastName() != null ? shopOwner.getLastName() : "Owner")
                                .phoneNumber(shopOwner.getPhone())
                                .role(swari.sewa.common.enums.UserRole.SHOP_OWNER)
                                .isActive(true)
                                .isEmailVerified(true)
                                .build());
                    }

                    String licenseNumber = shopOwner.getLicenseNumber();
                    if (licenseNumber == null || licenseNumber.trim().isEmpty() || shopRepository.existsByLicenseNumber(licenseNumber)) {
                        licenseNumber = "SHOP-" + shopOwner.getId() + "-" + System.currentTimeMillis();
                    }

                    Shop createdShop = Shop.builder()
                            .name((shopOwner.getShopName() != null && !shopOwner.getShopName().trim().isEmpty())
                                    ? shopOwner.getShopName()
                                    : (shopOwner.getCompanyName() != null && !shopOwner.getCompanyName().trim().isEmpty())
                                        ? shopOwner.getCompanyName()
                                        : (shopOwner.getFirstName() != null ? shopOwner.getFirstName() : "Shop") + " Shop")
                            .description("Auto-created shop for vehicle creation")
                            .licenseNumber(licenseNumber)
                            .addressLine1(shopOwner.getAddress())
                            .city(shopOwner.getCity() != null ? shopOwner.getCity() : "Unknown")
                            .state(shopOwner.getState() != null ? shopOwner.getState() : "Unknown")
                            .country(shopOwner.getCountry() != null ? shopOwner.getCountry() : "Unknown")
                            .phoneNumber(shopOwner.getShopPhone() != null ? shopOwner.getShopPhone() : shopOwner.getPhone())
                            .emailAddress(shopOwner.getShopEmail() != null ? shopOwner.getShopEmail() : shopOwner.getEmail())
                            .status(ShopStatus.ACTIVE)
                            .isFeatured(false)
                            .shopOwner(shopOwner)
                            .user(user)
                            .build();

                    createdShop = shopRepository.save(createdShop);
                    shopIdToUse = createdShop.getId();
                }
            }

            if (shopIdToUse == null || !shopRepository.existsById(shopIdToUse)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Shop not found");
                errorResponse.put("message", "No shop found for this account. Please ask superadmin to create/approve a shop for this account.");
                return ResponseEntity.status(404).body(errorResponse);
            }

            // Subscription access check: shop owners must have an ACTIVE or TRIAL subscription
            // to add vehicles. Super admins bypass this check.
            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                Long shopOwnerId = resolveCurrentShopOwnerIdOrNull();
                if (shopOwnerId != null) {
                    subscriptionAccessService.validateCanAddVehicle(shopOwnerId);
                }
            }

            // Parse vehicle JSON from request part or handle direct VehicleDto for backward compatibility
            VehicleDto vehicleDto;
            if (vehicleJson != null && !vehicleJson.trim().isEmpty()) {
                // Parse JSON from multipart request
                vehicleDto = objectMapper.readValue(vehicleJson, VehicleDto.class);
            } else {
                // Fallback for direct JSON requests (backward compatibility)
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Invalid request format",
                    "message", "Vehicle data is required"
                ));
            }

            // Handle file uploads
            if (mediaFiles != null && mediaFiles.length > 0) {
                List<String> imageUrls = storageService.storeAll(mediaFiles, StorageCategory.VEHICLE, null);
                
                // Set the first image as main image and add all to imageUrls
                if (!imageUrls.isEmpty()) {
                    vehicleDto.setMainImageUrl(imageUrls.get(0));
                    vehicleDto.setImageUrls(new java.util.HashSet<>(imageUrls));
                }
            }

            // Handle bluebook file uploads
            if (bluebookFiles != null && bluebookFiles.length > 0) {
                List<String> bluebookUrls = storageService.storeAll(bluebookFiles, StorageCategory.VEHICLE, null);
                // Store bluebook URLs in specifications or as a separate field
                // For now, we'll store them in the specifications JSON
                String existingSpecs = vehicleDto.getSpecifications();
                java.util.Map<String, Object> specs = existingSpecs != null ? 
                    objectMapper.readValue(existingSpecs, java.util.Map.class) : new java.util.HashMap<>();
                specs.put("bluebookUrls", bluebookUrls);
                vehicleDto.setSpecifications(objectMapper.writeValueAsString(specs));
            }

            // Handle customer document uploads
            if (sellerPassportPhoto != null && !sellerPassportPhoto.isEmpty()) {
                String passportPhotoUrl = storageService.store(sellerPassportPhoto, StorageCategory.VEHICLE, null);
                vehicleDto.setSellerPassportPhoto(passportPhotoUrl);
            }

            if (sellerCitizenshipFront != null && !sellerCitizenshipFront.isEmpty()) {
                String citizenshipFrontUrl = storageService.store(sellerCitizenshipFront, StorageCategory.VEHICLE, null);
                vehicleDto.setSellerCitizenshipFront(citizenshipFrontUrl);
            }

            if (sellerCitizenshipBack != null && !sellerCitizenshipBack.isEmpty()) {
                String citizenshipBackUrl = storageService.store(sellerCitizenshipBack, StorageCategory.VEHICLE, null);
                vehicleDto.setSellerCitizenshipBack(citizenshipBackUrl);
            }

            VehicleDto createdVehicle = vehicleService.createVehicle(vehicleDto, shopIdToUse);
            return ResponseEntity.ok(createdVehicle);
        } catch (swari.sewa.module.subscription.exception.SubscriptionRequiredException
                | swari.sewa.module.subscription.exception.SubscriptionLimitExceededException e) {
            // Let the GlobalExceptionHandler handle these with proper error codes
            throw e;
        } catch (Exception e) {
            // Log the error and return a proper error response
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create vehicle");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<VehicleDto> vehicle;

        if (hasRole(authentication, "SHOP_OWNER")) {
            Long shopId = resolveCurrentShopIdOrNull();
            if (shopId == null) {
                return ResponseEntity.notFound().build();
            }
            vehicle = vehicleService.getVehicleByIdForShop(id, shopId);
        } else {
            vehicle = vehicleService.getVehicleById(id);
        }

        return vehicle.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/increment-view")
    public ResponseEntity<VehicleDto> incrementViewCount(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.incrementViewCount(id);
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping("/{id}/increment-contact")
    @PreAuthorize("hasRole('PUBLIC') or hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> incrementContactCount(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.incrementContactCount(id);
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping
    public ResponseEntity<Page<VehicleDto>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getAllVehicles(page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<VehicleDto>> getActiveVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getActiveVehicles(page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/inactive")
    public ResponseEntity<Page<VehicleDto>> getInactiveVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getInactiveVehicles(page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/featured")
    public ResponseEntity<Page<VehicleDto>> getFeaturedVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getFeaturedVehicles(page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<VehicleDto>> getVehiclesByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long shopIdToUse = shopId;
        if (hasRole(authentication, "SHOP_OWNER")) {
            shopIdToUse = resolveCurrentShopIdOrNull();
            if (shopIdToUse == null) {
                return ResponseEntity.notFound().build();
            }
        }
        Page<VehicleDto> vehicles = vehicleService.getVehiclesByShop(shopIdToUse, page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<VehicleDto>> getVehiclesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getVehiclesByCategory(categoryId, page, size);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/type/{vehicleType}")
    public ResponseEntity<Page<VehicleDto>> getVehiclesByType(
            @PathVariable VehicleType vehicleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleDto> vehicles = vehicleService.getVehiclesByType(vehicleType, page, size);
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<VehicleDto>> searchVehicles(@Valid @RequestBody VehicleSearchRequest searchRequest) {
        Page<VehicleDto> vehicles = vehicleService.searchVehicles(searchRequest);
        return ResponseEntity.ok(vehicles);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleDto vehicleDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        VehicleDto updatedVehicle;
        if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
            Long shopId = resolveCurrentShopIdOrNull();
            if (shopId == null) {
                return ResponseEntity.notFound().build();
            }
            updatedVehicle = vehicleService.updateVehicleForShop(id, vehicleDto, shopId);
        } else {
            updatedVehicle = vehicleService.updateVehicle(id, vehicleDto);
        }
        return ResponseEntity.ok(updatedVehicle);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<VehicleDto> updateVehicleWithFiles(
            @PathVariable Long id,
            @RequestPart(value = "vehicle", required = false) String vehicleJson,
            @RequestPart(value = "mediaFiles", required = false) MultipartFile[] mediaFiles,
            @RequestPart(value = "bluebookFiles", required = false) MultipartFile[] bluebookFiles,
            @RequestPart(value = "sellerPassportPhoto", required = false) MultipartFile sellerPassportPhoto,
            @RequestPart(value = "sellerCitizenshipFront", required = false) MultipartFile sellerCitizenshipFront,
            @RequestPart(value = "sellerCitizenshipBack", required = false) MultipartFile sellerCitizenshipBack,
            @RequestPart(value = "imagesToDelete", required = false) String imagesToDeleteJson,
            @RequestPart(value = "bluebookImagesToDelete", required = false) String bluebookImagesToDeleteJson,
            @RequestPart(value = "deletePassportPhoto", required = false) String deletePassportPhoto,
            @RequestPart(value = "deleteCitizenshipFront", required = false) String deleteCitizenshipFront,
            @RequestPart(value = "deleteCitizenshipBack", required = false) String deleteCitizenshipBack,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Resolve shop ID
            Long shopIdToUse = shopId;
            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                shopIdToUse = resolveCurrentShopIdOrNull();
            }
            
            if (shopIdToUse == null) {
                return ResponseEntity.notFound().build();
            }

            // Parse vehicle JSON
            VehicleDto vehicleDto = objectMapper.readValue(vehicleJson, VehicleDto.class);
            
            // Handle image deletions
            if (imagesToDeleteJson != null && !imagesToDeleteJson.isEmpty()) {
                List<String> imagesToDelete = objectMapper.readValue(imagesToDeleteJson, List.class);
                Set<String> existingUrls = vehicleDto.getImageUrls() != null 
                    ? new HashSet<>(vehicleDto.getImageUrls()) 
                    : new HashSet<>();
                existingUrls.removeAll(imagesToDelete);
                vehicleDto.setImageUrls(existingUrls);
                imagesToDelete.forEach(storageService::deleteByUrl);
            }
            
            if (bluebookImagesToDeleteJson != null && !bluebookImagesToDeleteJson.isEmpty()) {
                List<String> bluebookImagesToDelete = objectMapper.readValue(bluebookImagesToDeleteJson, List.class);
                String existingSpecs = vehicleDto.getSpecifications();
                java.util.Map<String, Object> specs = existingSpecs != null 
                    ? objectMapper.readValue(existingSpecs, java.util.Map.class) 
                    : new java.util.HashMap<>();
                List<String> existingBluebookUrls = (List<String>) specs.getOrDefault("bluebookUrls", new ArrayList<>());
                existingBluebookUrls.removeAll(bluebookImagesToDelete);
                specs.put("bluebookUrls", existingBluebookUrls);
                vehicleDto.setSpecifications(objectMapper.writeValueAsString(specs));
                bluebookImagesToDelete.forEach(storageService::deleteByUrl);
            }
            
            if ("true".equals(deletePassportPhoto)) {
                if (vehicleDto.getSellerPassportPhoto() != null) {
                    storageService.deleteByUrl(vehicleDto.getSellerPassportPhoto());
                }
                vehicleDto.setSellerPassportPhoto(null);
            }
            if ("true".equals(deleteCitizenshipFront)) {
                if (vehicleDto.getSellerCitizenshipFront() != null) {
                    storageService.deleteByUrl(vehicleDto.getSellerCitizenshipFront());
                }
                vehicleDto.setSellerCitizenshipFront(null);
            }
            if ("true".equals(deleteCitizenshipBack)) {
                if (vehicleDto.getSellerCitizenshipBack() != null) {
                    storageService.deleteByUrl(vehicleDto.getSellerCitizenshipBack());
                }
                vehicleDto.setSellerCitizenshipBack(null);
            }
            
            // Handle file uploads
            if (mediaFiles != null && mediaFiles.length > 0) {
                List<String> imageUrls = storageService.storeAll(mediaFiles, StorageCategory.VEHICLE, id);
                if (!imageUrls.isEmpty()) {
                    Set<String> existingUrls = vehicleDto.getImageUrls() != null 
                        ? new HashSet<>(vehicleDto.getImageUrls()) 
                        : new HashSet<>();
                    existingUrls.addAll(imageUrls);
                    vehicleDto.setImageUrls(existingUrls);
                    if (vehicleDto.getMainImageUrl() == null || vehicleDto.getMainImageUrl().isEmpty()) {
                        vehicleDto.setMainImageUrl(imageUrls.get(0));
                    }
                }
            }

            if (bluebookFiles != null && bluebookFiles.length > 0) {
                List<String> bluebookUrls = storageService.storeAll(bluebookFiles, StorageCategory.VEHICLE, id);
                String existingSpecs = vehicleDto.getSpecifications();
                java.util.Map<String, Object> specs = existingSpecs != null 
                    ? objectMapper.readValue(existingSpecs, java.util.Map.class) 
                    : new java.util.HashMap<>();
                List<String> existingBluebookUrls = (List<String>) specs.getOrDefault("bluebookUrls", new ArrayList<>());
                existingBluebookUrls.addAll(bluebookUrls);
                specs.put("bluebookUrls", existingBluebookUrls);
                vehicleDto.setSpecifications(objectMapper.writeValueAsString(specs));
            }

            if (sellerPassportPhoto != null && !sellerPassportPhoto.isEmpty()) {
                String passportPhotoUrl = storageService.store(sellerPassportPhoto, StorageCategory.VEHICLE, id);
                vehicleDto.setSellerPassportPhoto(passportPhotoUrl);
            }

            if (sellerCitizenshipFront != null && !sellerCitizenshipFront.isEmpty()) {
                String citizenshipFrontUrl = storageService.store(sellerCitizenshipFront, StorageCategory.VEHICLE, id);
                vehicleDto.setSellerCitizenshipFront(citizenshipFrontUrl);
            }

            if (sellerCitizenshipBack != null && !sellerCitizenshipBack.isEmpty()) {
                String citizenshipBackUrl = storageService.store(sellerCitizenshipBack, StorageCategory.VEHICLE, id);
                vehicleDto.setSellerCitizenshipBack(citizenshipBackUrl);
            }

            VehicleDto updatedVehicle;
            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                updatedVehicle = vehicleService.updateVehicleForShop(id, vehicleDto, shopIdToUse);
            } else {
                updatedVehicle = vehicleService.updateVehicle(id, vehicleDto);
            }
            
            return ResponseEntity.ok(updatedVehicle);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update vehicle");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body((VehicleDto) errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
            Long shopId = resolveCurrentShopIdOrNull();
            if (shopId == null) {
                return ResponseEntity.notFound().build();
            }
            vehicleService.deleteVehicleForShop(id, shopId);
        } else {
            vehicleService.deleteVehicle(id);
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> approveVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.approveVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> rejectVehicle(@PathVariable Long id, @RequestParam String reason) {
        VehicleDto vehicle = vehicleService.rejectVehicle(id, reason);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> activateVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.activateVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> deactivateVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.deactivateVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> publishVehicle(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        VehicleDto vehicle;
        if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
            Long shopId = resolveCurrentShopIdOrNull();
            if (shopId == null) {
                return ResponseEntity.notFound().build();
            }
            vehicle = vehicleService.activateVehicle(id);
        } else {
            vehicle = vehicleService.activateVehicle(id);
        }
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    public ResponseEntity<VehicleDto> unpublishVehicle(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        VehicleDto vehicle;
        if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
            Long shopId = resolveCurrentShopIdOrNull();
            if (shopId == null) {
                return ResponseEntity.notFound().build();
            }
            vehicle = vehicleService.deactivateVehicle(id);
        } else {
            vehicle = vehicleService.deactivateVehicle(id);
        }
        return ResponseEntity.ok(vehicle);
    }

    
    @PutMapping(value = "/{id}/mark-sold", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPERADMIN')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<?> markAsSold(
            @PathVariable Long id,
            @RequestPart("customerData") swari.sewa.module.vehicle.dto.SellVehicleApplicationDto customerData,
            @RequestPart(value = "customerPhoto", required = false) MultipartFile customerPhoto,
            @RequestPart(value = "citizenshipFrontPhoto", required = false) MultipartFile citizenshipFrontPhoto,
            @RequestPart(value = "citizenshipBackPhoto", required = false) MultipartFile citizenshipBackPhoto,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        try {
            System.out.println("=== MARK-SOLD API CALLED ===");
            System.out.println("Vehicle ID: " + id);
            System.out.println("Shop ID from header: " + shopId);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Resolve shop ID from header or authentication
            Long shopIdToUse = shopId;
            if (shopIdToUse == null || !hasRole(authentication, "SUPERADMIN")) {
                shopIdToUse = resolveCurrentShopIdOrNull();
                System.out.println("Resolved shop ID from auth: " + shopIdToUse);
            }
            
            if (shopIdToUse == null) {
                throw new IllegalArgumentException("Shop ID could not be resolved. Please ensure you have a shop associated with your account.");
            }

            // Subscription access check: shop owners must have an ACTIVE or TRIAL subscription
            // to sell vehicles. Super admins bypass this check.
            if (hasRole(authentication, "SHOP_OWNER") && !hasRole(authentication, "SUPERADMIN")) {
                Long shopOwnerId = resolveCurrentShopOwnerIdOrNull();
                if (shopOwnerId != null) {
                    subscriptionAccessService.requireVehicleAccess(shopOwnerId);
                }
            }

            System.out.println("Final shop ID to use: " + shopIdToUse);
            
            // Debug: Log customer data
            System.out.println("Customer Data Received:");
            System.out.println("  Customer Name: " + customerData.getCustomerName());
            System.out.println("  Offered Price: " + customerData.getOfferedPrice());
            System.out.println("  Offered Price in Words: " + customerData.getOfferedPriceInWords());
            System.out.println("  Financing Amount: " + customerData.getFinancingAmount());
            System.out.println("  Down Payment: " + customerData.getDownPayment());
            
            // Store files and set URLs into DTO
            if (customerPhoto != null && !customerPhoto.isEmpty()) {
                String url = storageService.store(customerPhoto, StorageCategory.VEHICLE, id);
                customerData.setCustomerPhoto(url);
                System.out.println("Customer photo stored: " + url);
            }
            if (citizenshipFrontPhoto != null && !citizenshipFrontPhoto.isEmpty()) {
                String url = storageService.store(citizenshipFrontPhoto, StorageCategory.VEHICLE, id);
                customerData.setCitizenshipFrontPhoto(url);
                System.out.println("Citizenship front photo stored: " + url);
            }
            if (citizenshipBackPhoto != null && !citizenshipBackPhoto.isEmpty()) {
                String url = storageService.store(citizenshipBackPhoto, StorageCategory.VEHICLE, id);
                customerData.setCitizenshipBackPhoto(url);
                System.out.println("Citizenship back photo stored: " + url);
            }
            
            System.out.println("Calling vehicleService.markAsSold...");
            VehicleDto vehicle = vehicleService.markAsSold(id, customerData, shopIdToUse);
            System.out.println("Vehicle marked as sold successfully");
            return ResponseEntity.ok(vehicle);
        } catch (swari.sewa.module.subscription.exception.SubscriptionRequiredException
                | swari.sewa.module.subscription.exception.SubscriptionLimitExceededException e) {
            // Let the GlobalExceptionHandler handle these with proper error codes
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Exception in markAsSold: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to mark vehicle as sold");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<VehicleDto>> getPendingApprovalVehicles() {
        List<VehicleDto> vehicles = vehicleService.getPendingApprovalVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<VehicleDto>> getVehiclesByStatus(@PathVariable VehicleStatus status) {
        List<VehicleDto> vehicles = vehicleService.getVehiclesByStatus(status);
        return ResponseEntity.ok(vehicles);
    }
}
