package swari.sewa.module.homepage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.homepage.dto.*;
import swari.sewa.module.homepage.entity.*;
import swari.sewa.module.homepage.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HomePageService {

    private final HomeBrandRepository brandRepository;
    private final HomeFeaturedVehicleRepository featuredVehicleRepository;
    private final HomeBudgetRepository budgetRepository;
    private final HomeServiceRepository serviceRepository;
    private final HomeStoreRepository storeRepository;

    @Transactional(readOnly = true)
    public HomePageDto getHomePage() {
        return HomePageDto.builder()
                .brands(getActiveBrands())
                .featuredVehicles(getActiveFeaturedVehicles())
                .budgets(getActiveBudgets())
                .services(getActiveServices())
                .stores(getActiveStores())
                .build();
    }

    // Brands
    @Transactional(readOnly = true)
    public List<HomeBrandDto> getAllBrands() {
        return brandRepository.findAll().stream().map(this::toBrandDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HomeBrandDto> getActiveBrands() {
        return brandRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream().map(this::toBrandDto).collect(Collectors.toList());
    }

    public HomeBrandDto createBrand(HomeBrandDto dto) {
        HomeBrand brand = HomeBrand.builder()
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return toBrandDto(brandRepository.save(brand));
    }

    public HomeBrandDto updateBrand(Long id, HomeBrandDto dto) {
        HomeBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));
        if (dto.getName() != null) brand.setName(dto.getName());
        if (dto.getImageUrl() != null) brand.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) brand.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) brand.setDisplayOrder(dto.getDisplayOrder());
        return toBrandDto(brandRepository.save(brand));
    }

    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
    }

    // Featured Vehicles
    @Transactional(readOnly = true)
    public List<HomeFeaturedVehicleDto> getAllFeaturedVehicles() {
        return featuredVehicleRepository.findAll().stream().map(this::toFeaturedDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HomeFeaturedVehicleDto> getActiveFeaturedVehicles() {
        return featuredVehicleRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream().map(this::toFeaturedDto).collect(Collectors.toList());
    }

    public HomeFeaturedVehicleDto createFeaturedVehicle(HomeFeaturedVehicleDto dto) {
        HomeFeaturedVehicle featured = HomeFeaturedVehicle.builder()
                .vehicleId(dto.getVehicleId())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return toFeaturedDto(featuredVehicleRepository.save(featured));
    }

    public HomeFeaturedVehicleDto updateFeaturedVehicle(Long id, HomeFeaturedVehicleDto dto) {
        HomeFeaturedVehicle featured = featuredVehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Featured vehicle not found with id: " + id));
        if (dto.getVehicleId() != null) featured.setVehicleId(dto.getVehicleId());
        if (dto.getImageUrl() != null) featured.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) featured.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) featured.setDisplayOrder(dto.getDisplayOrder());
        return toFeaturedDto(featuredVehicleRepository.save(featured));
    }

    public void deleteFeaturedVehicle(Long id) {
        featuredVehicleRepository.deleteById(id);
    }

    // Budgets
    @Transactional(readOnly = true)
    public List<HomeBudgetDto> getAllBudgets() {
        return budgetRepository.findAll().stream().map(this::toBudgetDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HomeBudgetDto> getActiveBudgets() {
        return budgetRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream().map(this::toBudgetDto).collect(Collectors.toList());
    }

    public HomeBudgetDto createBudget(HomeBudgetDto dto) {
        HomeBudget budget = HomeBudget.builder()
                .title(dto.getTitle())
                .maxPrice(dto.getMaxPrice())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return toBudgetDto(budgetRepository.save(budget));
    }

    public HomeBudgetDto updateBudget(Long id, HomeBudgetDto dto) {
        HomeBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));
        if (dto.getTitle() != null) budget.setTitle(dto.getTitle());
        if (dto.getMaxPrice() != null) budget.setMaxPrice(dto.getMaxPrice());
        if (dto.getImageUrl() != null) budget.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) budget.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) budget.setDisplayOrder(dto.getDisplayOrder());
        return toBudgetDto(budgetRepository.save(budget));
    }

    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    // Stores
    @Transactional(readOnly = true)
    public List<HomeStoreDto> getAllStores() {
        return storeRepository.findAll().stream().map(this::toStoreDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HomeStoreDto> getActiveStores() {
        return storeRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream().map(this::toStoreDto).collect(Collectors.toList());
    }

    public HomeStoreDto createStore(HomeStoreDto dto) {
        HomeStore store = HomeStore.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .directionsUrl(dto.getDirectionsUrl())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return toStoreDto(storeRepository.save(store));
    }

    public HomeStoreDto updateStore(Long id, HomeStoreDto dto) {
        HomeStore store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found with id: " + id));
        if (dto.getName() != null) store.setName(dto.getName());
        if (dto.getAddress() != null) store.setAddress(dto.getAddress());
        if (dto.getPhone() != null) store.setPhone(dto.getPhone());
        if (dto.getDirectionsUrl() != null) store.setDirectionsUrl(dto.getDirectionsUrl());
        if (dto.getImageUrl() != null) store.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) store.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) store.setDisplayOrder(dto.getDisplayOrder());
        return toStoreDto(storeRepository.save(store));
    }

    public void deleteStore(Long id) {
        storeRepository.deleteById(id);
    }

    // Services
    @Transactional(readOnly = true)
    public List<HomeServiceDto> getAllServices() {
        return serviceRepository.findAll().stream().map(this::toServiceDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HomeServiceDto> getActiveServices() {
        return serviceRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream().map(this::toServiceDto).collect(Collectors.toList());
    }

    public HomeServiceDto createService(HomeServiceDto dto) {
        HomeService service = HomeService.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .redirectUrl(dto.getRedirectUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return toServiceDto(serviceRepository.save(service));
    }

    public HomeServiceDto updateService(Long id, HomeServiceDto dto) {
        HomeService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        if (dto.getTitle() != null) service.setTitle(dto.getTitle());
        if (dto.getDescription() != null) service.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) service.setImageUrl(dto.getImageUrl());
        if (dto.getRedirectUrl() != null) service.setRedirectUrl(dto.getRedirectUrl());
        if (dto.getIsActive() != null) service.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) service.setDisplayOrder(dto.getDisplayOrder());
        return toServiceDto(serviceRepository.save(service));
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    // Mappers
    private HomeBrandDto toBrandDto(HomeBrand b) {
        return HomeBrandDto.builder()
                .id(b.getId())
                .name(b.getName())
                .imageUrl(b.getImageUrl())
                .isActive(b.getIsActive())
                .displayOrder(b.getDisplayOrder())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private HomeFeaturedVehicleDto toFeaturedDto(HomeFeaturedVehicle v) {
        return HomeFeaturedVehicleDto.builder()
                .id(v.getId())
                .vehicleId(v.getVehicleId())
                .imageUrl(v.getImageUrl())
                .isActive(v.getIsActive())
                .displayOrder(v.getDisplayOrder())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private HomeBudgetDto toBudgetDto(HomeBudget b) {
        return HomeBudgetDto.builder()
                .id(b.getId())
                .title(b.getTitle())
                .maxPrice(b.getMaxPrice())
                .imageUrl(b.getImageUrl())
                .isActive(b.getIsActive())
                .displayOrder(b.getDisplayOrder())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private HomeServiceDto toServiceDto(HomeService s) {
        return HomeServiceDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .imageUrl(s.getImageUrl())
                .redirectUrl(s.getRedirectUrl())
                .isActive(s.getIsActive())
                .displayOrder(s.getDisplayOrder())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private HomeStoreDto toStoreDto(HomeStore s) {
        return HomeStoreDto.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .phone(s.getPhone())
                .directionsUrl(s.getDirectionsUrl())
                .imageUrl(s.getImageUrl())
                .isActive(s.getIsActive())
                .displayOrder(s.getDisplayOrder())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
