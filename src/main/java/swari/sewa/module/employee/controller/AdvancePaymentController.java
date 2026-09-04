package swari.sewa.module.employee.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.AdvancePaymentDto;
import swari.sewa.module.employee.dto.AdvancePaymentRequestDto;
import swari.sewa.module.employee.service.AdvancePaymentService;

@RestController
@RequestMapping("/api/advances")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdvancePaymentController {
    
    private final AdvancePaymentService advancePaymentService;
    
    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#requestDto.employeeId, authentication.name)")
    public ResponseEntity<AdvancePaymentDto> createAdvancePayment(@Valid @RequestBody AdvancePaymentRequestDto requestDto) {
        AdvancePaymentDto advance = advancePaymentService.createAdvancePayment(requestDto);
        return ResponseEntity.ok(advance);
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAdvanceOwner(#id, authentication.name)")
    public ResponseEntity<AdvancePaymentDto> approveAdvance(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        AdvancePaymentDto advance = advancePaymentService.approveAdvance(id, approvedBy);
        return ResponseEntity.ok(advance);
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAdvanceOwner(#id, authentication.name)")
    public ResponseEntity<AdvancePaymentDto> rejectAdvance(
            @PathVariable Long id,
            @RequestParam String rejectionReason) {
        AdvancePaymentDto advance = advancePaymentService.rejectAdvance(id, rejectionReason);
        return ResponseEntity.ok(advance);
    }
    
    @PutMapping("/{id}/recovery")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAdvanceOwner(#id, authentication.name)")
    public ResponseEntity<AdvancePaymentDto> updateRecovery(
            @PathVariable Long id,
            @RequestParam BigDecimal recoveredAmount) {
        AdvancePaymentDto advance = advancePaymentService.updateRecovery(id, recoveredAmount);
        return ResponseEntity.ok(advance);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAdvanceOwner(#id, authentication.name)")
    public ResponseEntity<AdvancePaymentDto> getAdvanceById(@PathVariable Long id) {
        return advancePaymentService.getAdvanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<List<AdvancePaymentDto>> getAdvanceByEmployee(@PathVariable Long employeeId) {
        List<AdvancePaymentDto> advances = advancePaymentService.getAdvanceByEmployee(employeeId);
        return ResponseEntity.ok(advances);
    }
    
    @GetMapping("/shop/{shopId}/pending")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AdvancePaymentDto>> getPendingAdvances(@PathVariable Long shopId) {
        List<AdvancePaymentDto> advances = advancePaymentService.getPendingAdvances(shopId);
        return ResponseEntity.ok(advances);
    }
    
    @GetMapping("/shop/{shopId}/active")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AdvancePaymentDto>> getActiveAdvances(@PathVariable Long shopId) {
        List<AdvancePaymentDto> advances = advancePaymentService.getActiveAdvances(shopId);
        return ResponseEntity.ok(advances);
    }

    // ── Advance summary: totals + active count (for KPIs without loading ALL advances) ──

    @GetMapping("/shop/{shopId}/summary")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<Map<String, Object>> getAdvanceSummary(@PathVariable Long shopId) {
        return ResponseEntity.ok(advancePaymentService.getAdvanceSummary(shopId));
    }
}
