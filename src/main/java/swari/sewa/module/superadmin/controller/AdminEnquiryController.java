package swari.sewa.module.superadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.superadmin.service.AdminEnquiryService;

@RestController
@RequestMapping("/api/superadmin/enquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminEnquiryController {

    private final AdminEnquiryService adminEnquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Object>>> getAllEnquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminEnquiryService.getAllEnquiries(pageable, status, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getEnquiryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminEnquiryService.getEnquiryById(id)));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<ApiResponse<String>> respondToEnquiry(
            @PathVariable Long id,
            @RequestBody String response) {
        adminEnquiryService.respondToEnquiry(id, response);
        return ResponseEntity.ok(ApiResponse.success("Response sent successfully"));
    }

    @PostMapping("/{id}/mark-responded")
    public ResponseEntity<ApiResponse<String>> markEnquiryAsResponded(@PathVariable Long id) {
        adminEnquiryService.markEnquiryAsResponded(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry marked as responded"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<String>> closeEnquiry(@PathVariable Long id) {
        adminEnquiryService.closeEnquiry(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry closed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEnquiry(@PathVariable Long id) {
        adminEnquiryService.deleteEnquiry(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry deleted successfully"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<Object>>> getPendingEnquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminEnquiryService.getPendingEnquiries(pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getEnquiryStats() {
        return ResponseEntity.ok(ApiResponse.success(adminEnquiryService.getEnquiryStats()));
    }
}
