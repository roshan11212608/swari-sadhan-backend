package swari.sewa.module.employee.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.dto.LeaveRequestRequestDto;
import swari.sewa.module.employee.service.LeaveService;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class LeaveController {
    
    private final LeaveService leaveService;
    
    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#requestDto.employeeId, authentication.name)")
    public ResponseEntity<LeaveRequestDto> createLeaveRequest(@Valid @RequestBody LeaveRequestRequestDto requestDto) {
        LeaveRequestDto leave = leaveService.createLeaveRequest(requestDto);
        return ResponseEntity.ok(leave);
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isLeaveOwner(#id, authentication.name)")
    public ResponseEntity<LeaveRequestDto> approveLeave(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        LeaveRequestDto leave = leaveService.approveLeave(id, approvedBy);
        return ResponseEntity.ok(leave);
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isLeaveOwner(#id, authentication.name)")
    public ResponseEntity<LeaveRequestDto> rejectLeave(
            @PathVariable Long id,
            @RequestParam String rejectionReason) {
        LeaveRequestDto leave = leaveService.rejectLeave(id, rejectionReason);
        return ResponseEntity.ok(leave);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isLeaveOwner(#id, authentication.name)")
    public ResponseEntity<LeaveRequestDto> getLeaveById(@PathVariable Long id) {
        return leaveService.getLeaveById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<List<LeaveRequestDto>> getLeaveByEmployee(@PathVariable Long employeeId) {
        List<LeaveRequestDto> leaves = leaveService.getLeaveByEmployee(employeeId);
        return ResponseEntity.ok(leaves);
    }
    
    @GetMapping("/shop/{shopId}/pending")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<LeaveRequestDto>> getPendingLeaves(@PathVariable Long shopId) {
        List<LeaveRequestDto> leaves = leaveService.getPendingLeaves(shopId);
        return ResponseEntity.ok(leaves);
    }
    
    @GetMapping("/shop/{shopId}/status/{status}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<LeaveRequestDto>> getLeavesByStatus(
            @PathVariable Long shopId,
            @PathVariable String status) {
        List<LeaveRequestDto> leaves = leaveService.getLeavesByStatus(shopId, status);
        return ResponseEntity.ok(leaves);
    }
}
