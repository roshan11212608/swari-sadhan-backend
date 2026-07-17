package swari.sewa.module.enquiry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.enquiry.dto.EnquiryDto;
import swari.sewa.module.enquiry.dto.EnquiryMessageDto;
import swari.sewa.common.enums.EnquiryMessageSender;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.enquiry.service.EnquiryService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/enquiries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    public ResponseEntity<EnquiryDto> createEnquiry(@Valid @RequestBody EnquiryDto enquiryDto) {
        EnquiryDto createdEnquiry = enquiryService.createEnquiry(enquiryDto);
        return ResponseEntity.ok(createdEnquiry);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name)) or (hasRole('PUBLIC') and @enquirySecurity.isCustomer(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> getEnquiryById(@PathVariable Long id) {
        Optional<EnquiryDto> enquiry = enquiryService.getEnquiryById(id);
        return enquiry.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Page<EnquiryDto>> getAllEnquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.getAllEnquiries(page, size);
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('PUBLIC') and @userSecurity.isOwner(#customerId, authentication.name))")
    public ResponseEntity<Page<EnquiryDto>> getEnquiriesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.getEnquiriesByCustomer(customerId, page, size);
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<EnquiryDto>> getEnquiriesByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.getEnquiriesByShop(shopId, page, size);
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @vehicleSecurity.isShopOwner(#vehicleId, authentication.name))")
    public ResponseEntity<Page<EnquiryDto>> getEnquiriesByVehicle(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.getEnquiriesByVehicle(vehicleId, page, size);
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwnerByStatus(#status, authentication.name))")
    public ResponseEntity<Page<EnquiryDto>> getEnquiriesByStatus(
            @PathVariable EnquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.getEnquiriesByStatus(status, page, size);
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/shop/{shopId}/pending")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name))")
    public ResponseEntity<List<EnquiryDto>> getPendingEnquiriesByShop(@PathVariable Long shopId) {
        List<EnquiryDto> enquiries = enquiryService.getPendingEnquiriesByShop(shopId);
        return ResponseEntity.ok(enquiries);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> updateEnquiry(@PathVariable Long id, @Valid @RequestBody EnquiryDto enquiryDto) {
        EnquiryDto updatedEnquiry = enquiryService.updateEnquiry(id, enquiryDto);
        return ResponseEntity.ok(updatedEnquiry);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> updateEnquiryStatus(
            @PathVariable Long id,
            @RequestBody EnquiryStatus status) {
        EnquiryDto updatedEnquiry = enquiryService.updateEnquiryStatus(id, status);
        return ResponseEntity.ok(updatedEnquiry);
    }

    @PutMapping("/{id}/contacted")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> markAsContacted(@PathVariable Long id) {
        EnquiryDto enquiry = enquiryService.markAsContacted(id);
        return ResponseEntity.ok(enquiry);
    }

    @PutMapping("/{id}/closed")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> markAsClosed(@PathVariable Long id) {
        EnquiryDto enquiry = enquiryService.markAsClosed(id);
        return ResponseEntity.ok(enquiry);
    }

    @PutMapping("/{id}/resolved")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<EnquiryDto> markAsResolved(@PathVariable Long id) {
        EnquiryDto enquiry = enquiryService.markAsResolved(id);
        return ResponseEntity.ok(enquiry);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name))")
    public ResponseEntity<Void> deleteEnquiry(@PathVariable Long id) {
        enquiryService.deleteEnquiry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name)) or (hasRole('PUBLIC') and @enquirySecurity.isCustomer(#id, authentication.name))")
    public ResponseEntity<List<EnquiryMessageDto>> getEnquiryMessages(@PathVariable Long id) {
        List<EnquiryMessageDto> messages = enquiryService.getEnquiryMessages(id);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('SHOP_OWNER') and @enquirySecurity.isShopOwner(#id, authentication.name)) or (hasRole('PUBLIC') and @enquirySecurity.isCustomer(#id, authentication.name))")
    public ResponseEntity<EnquiryMessageDto> addEnquiryMessage(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryMessageDto messageDto,
            Authentication authentication) {
        boolean isShopOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SHOP_OWNER"::equals);

        messageDto.setSender(isShopOwner ? EnquiryMessageSender.SHOP_OWNER : EnquiryMessageSender.CUSTOMER);
        EnquiryMessageDto savedMessage = enquiryService.addEnquiryMessage(id, messageDto);
        return ResponseEntity.ok(savedMessage);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Page<EnquiryDto>> searchEnquiries(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EnquiryDto> enquiries = enquiryService.searchEnquiries(keyword, page, size);
        return ResponseEntity.ok(enquiries);
    }
}
