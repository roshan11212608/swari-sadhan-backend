package swari.sewa.module.vehicle.service.impl;

import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import swari.sewa.module.vehicle.dto.VehicleDto;
import swari.sewa.common.dto.VehicleSearchRequest;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.RegistrationNumberAlreadyExistsException;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.entity.SellVehicleApplication;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.vehicle.repository.SellVehicleApplicationRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.category.repository.CategoryRepository;
import swari.sewa.module.vehicle.service.VehicleService;
import swari.sewa.module.vehicle.service.SellApplicationService;
import swari.sewa.module.vehicle.service.SellVehicleApplicationService;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.common.enums.PaymentMethod;
import swari.sewa.module.category.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final SellApplicationService sellApplicationService;
    private final SellVehicleApplicationService sellVehicleApplicationService;
    private final SellVehicleApplicationRepository sellVehicleApplicationRepository;

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public VehicleDto createVehicle(VehicleDto vehicleDto, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        Category category = null;
        Long categoryId = vehicleDto.getCategoryId();
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        if (category == null) {
            final String resolvedCategoryName =
                    (vehicleDto.getCategoryName() == null || vehicleDto.getCategoryName().trim().isEmpty())
                            ? "Bike"
                            : vehicleDto.getCategoryName().trim();

            category = categoryRepository.findByName(resolvedCategoryName)
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name(resolvedCategoryName)
                            .description("Auto-created category")
                            .isActive(true)
                            .build()));
        }

        if (vehicleDto.getRegistrationNumber() != null && 
            vehicleRepository.existsByRegistrationNumber(vehicleDto.getRegistrationNumber())) {
            throw new RegistrationNumberAlreadyExistsException("Registration number already exists: " + vehicleDto.getRegistrationNumber());
        }

        // Create Vehicle object manually to avoid ModelMapper field mapping issues
        Vehicle vehicle = new Vehicle();
        vehicle.setShop(shop);
        vehicle.setCategory(category);
        vehicle.setStatus(VehicleStatus.INACTIVE);
        vehicle.setViewCount(0L);
        vehicle.setContactCount(0L);

        // Map frontend fields to backend entity fields correctly
        // Frontend: price = cost price, sellPrice = selling price
        // Backend: purchasePrice = cost price, price = selling price
        if (vehicleDto.getSellPrice() != null) {
            vehicle.setPrice(vehicleDto.getSellPrice());
        } else {
            vehicle.setPrice(vehicleDto.getPrice()); // Fallback if sellPrice not provided
        }
        if (vehicleDto.getPrice() != null) {
            vehicle.setPurchasePrice(vehicleDto.getPrice());
        }

        // Map other fields manually
        vehicle.setTitle(vehicleDto.getTitle());
        vehicle.setDescription(vehicleDto.getDescription());
        vehicle.setBrandName(vehicleDto.getBrandName());
        vehicle.setModelName(vehicleDto.getModelName());
        vehicle.setManufacturingYear(vehicleDto.getManufacturingYear());
        vehicle.setRegistrationNumber(vehicleDto.getRegistrationNumber());
        vehicle.setLotsNumber(vehicleDto.getLotsNumber());
        vehicle.setVehicleType(vehicleDto.getVehicleType());
        vehicle.setFuelType(vehicleDto.getFuelType());
        vehicle.setTransmissionType(vehicleDto.getTransmissionType());
        vehicle.setBodyType(vehicleDto.getBodyType());
        vehicle.setColor(vehicleDto.getColor());
        vehicle.setKilometersDriven(vehicleDto.getKilometersDriven());
        vehicle.setEngineCapacity(vehicleDto.getEngineCapacity());
        vehicle.setIsNegotiable(vehicleDto.getIsNegotiable());
        vehicle.setCondition(vehicleDto.getCondition());
        vehicle.setOwnershipType(vehicleDto.getOwnershipType());
        vehicle.setInsuranceValid(vehicleDto.getInsuranceValid());
        vehicle.setLastServiceDate(vehicleDto.getLastServiceDate());
        vehicle.setMainImageUrl(vehicleDto.getMainImageUrl());
        vehicle.setImageUrls(vehicleDto.getImageUrls());
        vehicle.setVideoUrl(vehicleDto.getVideoUrl());
        vehicle.setSellerPassportPhoto(vehicleDto.getSellerPassportPhoto());
        vehicle.setSellerCitizenshipFront(vehicleDto.getSellerCitizenshipFront());
        vehicle.setSellerCitizenshipBack(vehicleDto.getSellerCitizenshipBack());
        vehicle.setSpecifications(vehicleDto.getSpecifications());
        vehicle.setFeatures(vehicleDto.getFeatures());
        vehicle.setRejectionReason(vehicleDto.getRejectionReason());
        vehicle.setIsFeatured(vehicleDto.getIsFeatured());
        vehicle.setAdditionalExpenses(vehicleDto.getAdditionalExpenditure() != null ? vehicleDto.getAdditionalExpenditure() : BigDecimal.ZERO);

        // Explicitly set boughtDate to ensure it's preserved for analytics
        if (vehicleDto.getBoughtDate() != null) {
            vehicle.setBoughtDate(vehicleDto.getBoughtDate());
        } else {
            // Default to current date if not provided
            vehicle.setBoughtDate(LocalDate.now());
        }
        
        // Explicitly set lotsNumber to ensure it's preserved
        if (vehicleDto.getLotsNumber() != null) {
            vehicle.setLotsNumber(vehicleDto.getLotsNumber());
        }
        
        // Explicitly set imageUrls to ensure it's preserved
        if (vehicleDto.getImageUrls() != null) {
            vehicle.setImageUrls(vehicleDto.getImageUrls());
        }
        
        // Explicitly set mainImageUrl
        if (vehicleDto.getMainImageUrl() != null) {
            vehicle.setMainImageUrl(vehicleDto.getMainImageUrl());
        }
        
        // Explicitly set customer photos and documents
        if (vehicleDto.getSellerPassportPhoto() != null) {
            vehicle.setSellerPassportPhoto(vehicleDto.getSellerPassportPhoto());
        }
        if (vehicleDto.getSellerCitizenshipFront() != null) {
            vehicle.setSellerCitizenshipFront(vehicleDto.getSellerCitizenshipFront());
        }
        if (vehicleDto.getSellerCitizenshipBack() != null) {
            vehicle.setSellerCitizenshipBack(vehicleDto.getSellerCitizenshipBack());
        }

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleDto> getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleDto> getVehicleByIdForShop(Long id, Long shopId) {
        return vehicleRepository.findByIdAndShopId(id, shopId)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getAllVehicles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findByStatusNot(VehicleStatus.SOLD, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getVehiclesByShop(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findByShopId(shopId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getVehiclesByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getVehiclesByType(VehicleType vehicleType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findByVehicleType(vehicleType, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getActiveVehicles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findActiveVehicles(pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getInactiveVehicles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findInactiveVehicles(pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> getFeaturedVehicles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findFeaturedVehicles(pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDto> searchVehicles(VehicleSearchRequest searchRequest) {
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());
        
        VehicleType vehicleType = null;
        if (searchRequest.getVehicleType() != null) {
            vehicleType = VehicleType.valueOf(searchRequest.getVehicleType().toUpperCase());
        }

        return vehicleRepository.searchVehicles(
                searchRequest.getBrand(),
                searchRequest.getModel(),
                vehicleType,
                searchRequest.getFuelType(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getMinYear(),
                searchRequest.getMaxYear(),
                searchRequest.getMinKilometers(),
                searchRequest.getMaxKilometers(),
                searchRequest.getCity(),
                pageable
        ).map(this::mapToDtoWithDetails);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public VehicleDto updateVehicle(Long id, VehicleDto vehicleDto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (vehicleDto.getRegistrationNumber() != null && 
            !vehicle.getRegistrationNumber().equals(vehicleDto.getRegistrationNumber()) &&
            vehicleRepository.existsByRegistrationNumber(vehicleDto.getRegistrationNumber())) {
            throw new RegistrationNumberAlreadyExistsException("Registration number already exists: " + vehicleDto.getRegistrationNumber());
        }

        if (vehicleDto.getCategoryId() != null && !vehicleDto.getCategoryId().equals(vehicle.getCategory().getId())) {
            Category category = categoryRepository.findById(vehicleDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + vehicleDto.getCategoryId()));
            vehicle.setCategory(category);
        }

        modelMapper.map(vehicleDto, vehicle);
        
        // Explicitly preserve lotsNumber
        if (vehicleDto.getLotsNumber() != null) {
            vehicle.setLotsNumber(vehicleDto.getLotsNumber());
        }
        
        // Explicitly preserve additionalExpenses
        if (vehicleDto.getAdditionalExpenditure() != null) {
            vehicle.setAdditionalExpenses(vehicleDto.getAdditionalExpenditure());
        }
        
        // Explicitly preserve boughtDate
        if (vehicleDto.getBoughtDate() != null) {
            vehicle.setBoughtDate(vehicleDto.getBoughtDate());
        }
        
        // Explicitly preserve imageUrls
        if (vehicleDto.getImageUrls() != null) {
            vehicle.setImageUrls(vehicleDto.getImageUrls());
        }
        
        // Explicitly preserve mainImageUrl
        if (vehicleDto.getMainImageUrl() != null) {
            vehicle.setMainImageUrl(vehicleDto.getMainImageUrl());
        }
        
        // Explicitly update customer photos and documents (including null for deletion)
        vehicle.setSellerPassportPhoto(vehicleDto.getSellerPassportPhoto());
        vehicle.setSellerCitizenshipFront(vehicleDto.getSellerCitizenshipFront());
        vehicle.setSellerCitizenshipBack(vehicleDto.getSellerCitizenshipBack());
        
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public VehicleDto updateVehicleForShop(Long id, VehicleDto vehicleDto, Long shopId) {
        Vehicle vehicle = vehicleRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (vehicleDto.getRegistrationNumber() != null &&
            !vehicle.getRegistrationNumber().equals(vehicleDto.getRegistrationNumber()) &&
            vehicleRepository.existsByRegistrationNumber(vehicleDto.getRegistrationNumber())) {
            throw new RegistrationNumberAlreadyExistsException("Registration number already exists: " + vehicleDto.getRegistrationNumber());
        }

        if (vehicleDto.getCategoryId() != null && !vehicleDto.getCategoryId().equals(vehicle.getCategory().getId())) {
            Category category = categoryRepository.findById(vehicleDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + vehicleDto.getCategoryId()));
            vehicle.setCategory(category);
        }

        modelMapper.map(vehicleDto, vehicle);
        
        // Explicitly preserve lotsNumber
        if (vehicleDto.getLotsNumber() != null) {
            vehicle.setLotsNumber(vehicleDto.getLotsNumber());
        }
        
        // Explicitly preserve additionalExpenses
        if (vehicleDto.getAdditionalExpenditure() != null) {
            vehicle.setAdditionalExpenses(vehicleDto.getAdditionalExpenditure());
        }
        
        // Explicitly preserve boughtDate
        if (vehicleDto.getBoughtDate() != null) {
            vehicle.setBoughtDate(vehicleDto.getBoughtDate());
        }
        
        // Explicitly preserve imageUrls
        if (vehicleDto.getImageUrls() != null) {
            vehicle.setImageUrls(vehicleDto.getImageUrls());
        }
        
        // Explicitly preserve mainImageUrl
        if (vehicleDto.getMainImageUrl() != null) {
            vehicle.setMainImageUrl(vehicleDto.getMainImageUrl());
        }
        
        // Explicitly update customer photos and documents (including null for deletion)
        vehicle.setSellerPassportPhoto(vehicleDto.getSellerPassportPhoto());
        vehicle.setSellerCitizenshipFront(vehicleDto.getSellerCitizenshipFront());
        vehicle.setSellerCitizenshipBack(vehicleDto.getSellerCitizenshipBack());
        
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicleRepository.delete(vehicle);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public void deleteVehicleForShop(Long id, Long shopId) {
        Vehicle vehicle = vehicleRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicleRepository.delete(vehicle);
    }

    @Override
    public VehicleDto approveVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRejectionReason(null);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    public VehicleDto rejectVehicle(Long id, String reason) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(VehicleStatus.REJECTED);
        vehicle.setRejectionReason(reason);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    public VehicleDto activateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        
        // Prevent re-publishing of SOLD vehicles
        if (vehicle.getStatus() == VehicleStatus.SOLD) {
            throw new IllegalStateException("Cannot publish a vehicle that has been sold. Vehicle ID: " + id);
        }
        
        vehicle.setStatus(VehicleStatus.ACTIVE);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    public VehicleDto deactivateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(VehicleStatus.INACTIVE);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public VehicleDto markAsSold(Long id, SellVehicleApplicationDto customerData, Long shopId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(VehicleStatus.SOLD);

        // Update vehicle price to actual selling price for accurate Sales Value calculation
        if (customerData.getOfferedPrice() != null) {
            vehicle.setPrice(customerData.getOfferedPrice());
        }

        // Use the sale date from customer data if provided, otherwise use current time
        java.time.LocalDateTime saleDate = customerData.getApplicationDate() != null
                ? customerData.getApplicationDate()
                : java.time.LocalDateTime.now();
        vehicle.setSoldAt(saleDate);

        // Use the provided shopId instead of getting from vehicle
        if (shopId == null) {
            throw new IllegalArgumentException("Shop ID is required");
        }

        // Check if there's an existing PENDING application for this vehicle
        List<SellVehicleApplication> existingApplications = sellVehicleApplicationRepository.findByVehicleId(id);
        SellVehicleApplication existingApplication = existingApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (existingApplication != null) {
            // Update existing PENDING application to COMPLETED
            existingApplication.setStatus(ApplicationStatus.COMPLETED);
            existingApplication.setCustomerName(customerData.getCustomerName());
            existingApplication.setCustomerParentName(customerData.getCustomerParentName());
            existingApplication.setCustomerPhone(customerData.getCustomerPhone());
            existingApplication.setCustomerEmail(customerData.getCustomerEmail());
            existingApplication.setCustomerAddress(customerData.getCustomerAddress());
            existingApplication.setCustomerCitizenshipNumber(customerData.getCustomerCitizenshipNumber());
            existingApplication.setCustomerPhoto(customerData.getCustomerPhoto());
            existingApplication.setCitizenshipFrontPhoto(customerData.getCitizenshipFrontPhoto());
            existingApplication.setCitizenshipBackPhoto(customerData.getCitizenshipBackPhoto());
            existingApplication.setApplicationDate(saleDate);
            existingApplication.setOfferedPrice(customerData.getOfferedPrice() != null ? customerData.getOfferedPrice() : vehicle.getPrice());
            existingApplication.setOfferedPriceInWords(customerData.getOfferedPriceInWords());
            existingApplication.setPaymentMethod(customerData.getPaymentMethod());
            existingApplication.setDownPayment(customerData.getDownPayment());
            existingApplication.setFinancingRequired(customerData.getFinancingRequired());
            existingApplication.setFinancingBank(customerData.getFinancingBank());
            existingApplication.setFinancingAmount(customerData.getFinancingAmount());
            existingApplication.setSalesManName(customerData.getSalesManName());
            existingApplication.setTermsAccepted(customerData.getTermsAccepted());
            existingApplication.setBackgroundCheckConsent(customerData.getBackgroundCheckConsent());
            existingApplication.setAddressProofProvided(customerData.getAddressProofProvided());
            existingApplication.setIncomeProofProvided(customerData.getIncomeProofProvided());
            existingApplication.setNotes(customerData.getNotes());
            existingApplication.setUpdatedAt(java.time.LocalDateTime.now());
            sellVehicleApplicationRepository.save(existingApplication);
        } else {
            // Create a new sell application with COMPLETED status
            SellVehicleApplicationDto sellApplication = SellVehicleApplicationDto.builder()
                    .vehicleId(id)
                    .shopId(shopId)
                    .vehicleBrand(vehicle.getBrandName())
                    .vehicleModel(vehicle.getModelName())
                    .vehiclePrice(vehicle.getPrice())
                    .customerName(customerData.getCustomerName())
                    .customerParentName(customerData.getCustomerParentName())
                    .customerPhone(customerData.getCustomerPhone())
                    .customerEmail(customerData.getCustomerEmail())
                    .customerAddress(customerData.getCustomerAddress())
                    .customerCitizenshipNumber(customerData.getCustomerCitizenshipNumber())
                    .customerPhoto(customerData.getCustomerPhoto())
                    .citizenshipFrontPhoto(customerData.getCitizenshipFrontPhoto())
                    .citizenshipBackPhoto(customerData.getCitizenshipBackPhoto())
                    .applicationDate(saleDate)
                    .offeredPrice(customerData.getOfferedPrice() != null ? customerData.getOfferedPrice() : vehicle.getPrice())
                    .offeredPriceInWords(customerData.getOfferedPriceInWords())
                    .paymentMethod(customerData.getPaymentMethod())
                    .downPayment(customerData.getDownPayment())
                    .financingRequired(customerData.getFinancingRequired())
                    .financingBank(customerData.getFinancingBank())
                    .financingAmount(customerData.getFinancingAmount())
                    .salesManName(customerData.getSalesManName())
                    .termsAccepted(customerData.getTermsAccepted())
                    .backgroundCheckConsent(customerData.getBackgroundCheckConsent())
                    .addressProofProvided(customerData.getAddressProofProvided())
                    .incomeProofProvided(customerData.getIncomeProofProvided())
                    .notes(customerData.getNotes())
                    .status(ApplicationStatus.COMPLETED)
                    .submittedAt(java.time.LocalDateTime.now())
                    .build();

            try {
                sellVehicleApplicationService.createApplication(sellApplication, id, shopId);
            } catch (Exception e) {
                // If application creation fails, rollback vehicle status
                vehicle.setStatus(VehicleStatus.ACTIVE);
                vehicleRepository.save(vehicle);
                throw new RuntimeException("Failed to create sell application: " + e.getMessage(), e);
            }
        }

        vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(vehicle);
    }

    @Override
    public VehicleDto incrementViewCount(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setViewCount(vehicle.getViewCount() + 1);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    public VehicleDto incrementContactCount(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setContactCount(vehicle.getContactCount() + 1);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDto> getPendingApprovalVehicles() {
        return vehicleRepository.findPendingApprovalVehicles().stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDto> getVehiclesByStatus(VehicleStatus status) {
        return vehicleRepository.findByShopIdAndStatus(null, status).stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return vehicleRepository.existsByRegistrationNumber(registrationNumber);
    }

    private VehicleDto mapToDtoWithDetails(Vehicle vehicle) {
        VehicleDto dto = modelMapper.map(vehicle, VehicleDto.class);
        dto.setShopId(vehicle.getShop().getId());
        dto.setShopName(vehicle.getShop().getName());
        dto.setShopCity(vehicle.getShop().getCity());
        dto.setShopPhone(vehicle.getShop().getPhoneNumber());
        dto.setShopEmail(vehicle.getShop().getEmailAddress());
        dto.setShopAddress(vehicle.getShop().getAddress());
        // Get shop owner address details
        if (vehicle.getShop().getShopOwner() != null) {
            dto.setShopProvince(vehicle.getShop().getShopOwner().getProvince());
            dto.setShopDistrict(vehicle.getShop().getShopOwner().getDistrict());
            dto.setShopMunicipality(vehicle.getShop().getShopOwner().getMunicipality());
            dto.setShopWard(vehicle.getShop().getShopOwner().getWard());
            dto.setShopTole(vehicle.getShop().getShopOwner().getTole());
        }
        dto.setCategoryId(vehicle.getCategory().getId());
        dto.setCategoryName(vehicle.getCategory().getName());
        // Explicitly set status to ensure it's included
        dto.setStatus(vehicle.getStatus());
        // Explicitly set lotsNumber to ensure it's included
        dto.setLotsNumber(vehicle.getLotsNumber());
        // Explicitly set additionalExpenditure to ensure it's included
        dto.setAdditionalExpenditure(vehicle.getAdditionalExpenses());
        // Explicitly set boughtDate to ensure it's included
        dto.setBoughtDate(vehicle.getBoughtDate());
        // Explicitly set imageUrls to ensure it's included
        dto.setImageUrls(vehicle.getImageUrls());
        // Explicitly set mainImageUrl
        dto.setMainImageUrl(vehicle.getMainImageUrl());
        // Explicitly set customer photos and documents
        dto.setSellerPassportPhoto(vehicle.getSellerPassportPhoto());
        dto.setSellerCitizenshipFront(vehicle.getSellerCitizenshipFront());
        dto.setSellerCitizenshipBack(vehicle.getSellerCitizenshipBack());
        // Explicitly set specifications and features to ensure they're included
        dto.setSpecifications(vehicle.getSpecifications());
        dto.setFeatures(vehicle.getFeatures());
        // Explicitly set other fields that might not be mapped correctly
        dto.setEngineCapacity(vehicle.getEngineCapacity());
        dto.setVideoUrl(vehicle.getVideoUrl());
        dto.setInsuranceValid(vehicle.getInsuranceValid());
        dto.setLastServiceDate(vehicle.getLastServiceDate());

        // Map backend fields to frontend fields correctly
        // Backend: purchasePrice = cost price, price = selling price
        // Frontend: price = cost price, sellPrice = selling price
        dto.setPrice(vehicle.getPurchasePrice()); // Frontend price = backend purchasePrice (cost price)
        dto.setSellPrice(vehicle.getPrice()); // Frontend sellPrice = backend price (selling price)
        dto.setPurchasePrice(vehicle.getPurchasePrice()); // Also set purchasePrice field for reference
        dto.setOwnershipType(vehicle.getOwnershipType());
        dto.setIsFeatured(vehicle.getIsFeatured());
        dto.setRejectionReason(vehicle.getRejectionReason());
        return dto;
    }
}
