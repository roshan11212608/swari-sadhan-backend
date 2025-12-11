package swari.sewa.module.publicuser.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.publicuser.dto.EnquiryDto;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.publicuser.model.Enquiry;
import swari.sewa.module.publicuser.model.User;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.publicuser.repository.EnquiryRepository;
import swari.sewa.module.publicuser.repository.UserRepository;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.common.enums.EnquiryStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    @Override
    public EnquiryDto createEnquiry(EnquiryDto enquiryDto) {
        User customer = userRepository.findById(enquiryDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + enquiryDto.getCustomerId()));

        Vehicle vehicle = vehicleRepository.findById(enquiryDto.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + enquiryDto.getVehicleId()));

        Shop shop = vehicleRepository.findById(enquiryDto.getVehicleId())
                .map(v -> v.getShop())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for vehicle"));

        Enquiry enquiry = modelMapper.map(enquiryDto, Enquiry.class);
        enquiry.setCustomer(customer);
        enquiry.setVehicle(vehicle);
        enquiry.setShop(shop);
        enquiry.setStatus(EnquiryStatus.PENDING);

        Enquiry savedEnquiry = enquiryRepository.save(enquiry);
        return mapToDtoWithDetails(savedEnquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnquiryDto> getEnquiryById(Long id) {
        return enquiryRepository.findById(id)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> getAllEnquiries(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.findAll(pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> getEnquiriesByCustomer(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> getEnquiriesByShop(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.findByShopId(shopId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> getEnquiriesByVehicle(Long vehicleId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.findByVehicleId(vehicleId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> getEnquiriesByStatus(EnquiryStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.findByStatus(status, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnquiryDto> getPendingEnquiriesByShop(Long shopId) {
        return enquiryRepository.findByShopIdAndStatus(shopId, EnquiryStatus.PENDING).stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    public EnquiryDto updateEnquiry(Long id, EnquiryDto enquiryDto) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));

        modelMapper.map(enquiryDto, enquiry);
        Enquiry updatedEnquiry = enquiryRepository.save(enquiry);
        return mapToDtoWithDetails(updatedEnquiry);
    }

    @Override
    public EnquiryDto updateEnquiryStatus(Long id, EnquiryStatus status) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        enquiry.setStatus(status);
        Enquiry updatedEnquiry = enquiryRepository.save(enquiry);
        return mapToDtoWithDetails(updatedEnquiry);
    }

    @Override
    public void deleteEnquiry(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        enquiryRepository.delete(enquiry);
    }

    @Override
    public EnquiryDto markAsContacted(Long id) {
        return updateEnquiryStatus(id, EnquiryStatus.CONTACTED);
    }

    @Override
    public EnquiryDto markAsClosed(Long id) {
        return updateEnquiryStatus(id, EnquiryStatus.CLOSED);
    }

    @Override
    public EnquiryDto markAsResolved(Long id) {
        return updateEnquiryStatus(id, EnquiryStatus.RESOLVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnquiryDto> getEnquiriesByStatus(EnquiryStatus status) {
        return enquiryRepository.findByShopIdAndStatus(null, status).stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryDto> searchEnquiries(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enquiryRepository.searchByKeyword(keyword, pageable)
                .map(this::mapToDtoWithDetails);
    }

    private EnquiryDto mapToDtoWithDetails(Enquiry enquiry) {
        EnquiryDto dto = modelMapper.map(enquiry, EnquiryDto.class);
        dto.setCustomerId(enquiry.getCustomer().getId());
        dto.setCustomerName(enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
        dto.setVehicleId(enquiry.getVehicle().getId());
        dto.setVehicleTitle(enquiry.getVehicle().getTitle());
        dto.setShopId(enquiry.getShop().getId());
        dto.setShopName(enquiry.getShop().getName());
        return dto;
    }
}
