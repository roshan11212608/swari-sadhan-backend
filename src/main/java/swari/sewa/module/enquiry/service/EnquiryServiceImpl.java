package swari.sewa.module.enquiry.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.enquiry.dto.EnquiryDto;
import swari.sewa.module.enquiry.dto.EnquiryMessageDto;
import swari.sewa.common.enums.EnquiryMessageSender;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.entity.EnquiryMessage;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.enquiry.repository.EnquiryMessageRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.common.enums.EnquiryStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EnquiryMessageRepository enquiryMessageRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    @Override
    public EnquiryDto createEnquiry(EnquiryDto enquiryDto) {
        Vehicle vehicle = vehicleRepository.findById(enquiryDto.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + enquiryDto.getVehicleId()));

        Shop shop = vehicleRepository.findById(enquiryDto.getVehicleId())
                .map(v -> v.getShop())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for vehicle"));

        // Create enquiry manually to avoid ModelMapper issues with transient User objects
        Enquiry enquiry = new Enquiry();
        enquiry.setCustomerName(enquiryDto.getCustomerName());
        enquiry.setCustomerEmail(enquiryDto.getCustomerEmail());
        enquiry.setCustomerPhone(enquiryDto.getCustomerPhone());
        enquiry.setMessage(enquiryDto.getMessage());
        enquiry.setPreferredContactMethod(enquiryDto.getPreferredContactMethod());
        enquiry.setBudgetRange(enquiryDto.getBudgetRange());
        enquiry.setExpectedPurchaseTime(enquiryDto.getExpectedPurchaseTime());
        enquiry.setFinancingRequired(enquiryDto.getFinancingRequired());
        enquiry.setTestDriveRequested(enquiryDto.getTestDriveRequested());
        enquiry.setAdminNotes(enquiryDto.getAdminNotes());
        
        // Handle guest enquiries (customerId is null)
        if (enquiryDto.getCustomerId() != null) {
            User customer = userRepository.findById(enquiryDto.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + enquiryDto.getCustomerId()));
            enquiry.setCustomer(customer);
        }
        
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
        
        // Handle guest enquiries where customer is null
        if (enquiry.getCustomer() != null) {
            dto.setCustomerId(enquiry.getCustomer().getId());
            dto.setCustomerName(enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
            // Use phone and email from enquiry entity (set during creation), not from User entity
            dto.setCustomerPhone(enquiry.getCustomerPhone());
            dto.setCustomerEmail(enquiry.getCustomerEmail());
        } else {
            // For guest enquiries, use the name/email/phone from the enquiry itself
            dto.setCustomerId(null);
            dto.setCustomerName(enquiry.getCustomerName());
            dto.setCustomerPhone(enquiry.getCustomerPhone());
            dto.setCustomerEmail(enquiry.getCustomerEmail());
        }
        
        dto.setVehicleId(enquiry.getVehicle().getId());
        dto.setVehicleTitle(enquiry.getVehicle().getTitle());
        dto.setShopId(enquiry.getShop().getId());
        dto.setShopName(enquiry.getShop().getName());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnquiryMessageDto> getEnquiryMessages(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        // Always surface the original enquiry message as the first chat message
        List<EnquiryMessageDto> result = new ArrayList<>();
        if (enquiry.getMessage() != null && !enquiry.getMessage().isBlank()) {
            result.add(EnquiryMessageDto.builder()
                    .enquiryId(enquiryId)
                    .sender(EnquiryMessageSender.CUSTOMER)
                    .senderName(enquiry.getCustomerName())
                    .message(enquiry.getMessage())
                    .createdAt(enquiry.getCreatedAt())
                    .build());
        }

        result.addAll(enquiryMessageRepository.findByEnquiryIdOrderByCreatedAtAsc(enquiryId).stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public EnquiryMessageDto addEnquiryMessage(Long enquiryId, EnquiryMessageDto messageDto) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        EnquiryMessage message = EnquiryMessage.builder()
                .enquiry(enquiry)
                .sender(messageDto.getSender())
                .senderName(resolveSenderName(enquiry, messageDto.getSender()))
                .message(messageDto.getMessage())
                .build();

        if (messageDto.getSender() == EnquiryMessageSender.SHOP_OWNER && enquiry.getStatus() == EnquiryStatus.PENDING) {
            enquiry.setStatus(EnquiryStatus.IN_PROGRESS);
        }

        EnquiryMessage savedMessage = enquiryMessageRepository.save(message);
        return mapMessageToDto(savedMessage);
    }

    private String resolveSenderName(Enquiry enquiry, EnquiryMessageSender sender) {
        if (sender == EnquiryMessageSender.SHOP_OWNER) {
            return enquiry.getShop() != null ? enquiry.getShop().getName() : "Shop Owner";
        }

        if (enquiry.getCustomer() != null) {
            return enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName();
        }

        return enquiry.getCustomerName() != null ? enquiry.getCustomerName() : "Customer";
    }

    private EnquiryMessageDto mapMessageToDto(EnquiryMessage message) {
        return EnquiryMessageDto.builder()
                .id(message.getId())
                .enquiryId(message.getEnquiry().getId())
                .sender(message.getSender())
                .senderName(message.getSenderName())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
