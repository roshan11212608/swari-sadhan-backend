package swari.sewa.module.shopowner.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shopowner.dto.VehicleDto;
import swari.sewa.common.dto.VehicleSearchRequest;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.RegistrationNumberAlreadyExistsException;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.superadmin.model.Category;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.module.shopowner.repository.ShopRepository;
import swari.sewa.module.superadmin.repository.CategoryRepository;
import swari.sewa.module.shopowner.service.VehicleService;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;

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

    @Override
    public VehicleDto createVehicle(VehicleDto vehicleDto, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        Category category = categoryRepository.findById(vehicleDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + vehicleDto.getCategoryId()));

        if (vehicleDto.getRegistrationNumber() != null && 
            vehicleRepository.existsByRegistrationNumber(vehicleDto.getRegistrationNumber())) {
            throw new RegistrationNumberAlreadyExistsException("Registration number already exists: " + vehicleDto.getRegistrationNumber());
        }

        Vehicle vehicle = modelMapper.map(vehicleDto, Vehicle.class);
        vehicle.setShop(shop);
        vehicle.setCategory(category);
        vehicle.setStatus(VehicleStatus.PENDING_APPROVAL);
        vehicle.setViewCount(0L);
        vehicle.setContactCount(0L);

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
    public Page<VehicleDto> getAllVehicles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vehicleRepository.findAll(pageable)
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
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
    }

    @Override
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
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
    public VehicleDto markAsSold(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(VehicleStatus.SOLD);
        vehicle.setSoldAt(java.time.LocalDateTime.now());
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDtoWithDetails(updatedVehicle);
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
        dto.setCategoryId(vehicle.getCategory().getId());
        dto.setCategoryName(vehicle.getCategory().getName());
        return dto;
    }
}
