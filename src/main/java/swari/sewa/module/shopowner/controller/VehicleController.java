package swari.sewa.module.shopowner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.shopowner.dto.VehicleDto;
import swari.sewa.common.dto.VehicleSearchRequest;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.shopowner.service.VehicleService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody VehicleDto vehicleDto,
                                                   @RequestHeader("X-Shop-Id") Long shopId) {
        VehicleDto createdVehicle = vehicleService.createVehicle(vehicleDto, shopId);
        return ResponseEntity.ok(createdVehicle);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable Long id) {
        Optional<VehicleDto> vehicle = vehicleService.getVehicleById(id);
        return vehicle.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/increment-view")
    public ResponseEntity<VehicleDto> incrementViewCount(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.incrementViewCount(id);
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping("/{id}/increment-contact")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
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
        Page<VehicleDto> vehicles = vehicleService.getVehiclesByShop(shopId, page, size);
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleDto vehicleDto) {
        VehicleDto updatedVehicle = vehicleService.updateVehicle(id, vehicleDto);
        return ResponseEntity.ok(updatedVehicle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> approveVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.approveVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> rejectVehicle(@PathVariable Long id, @RequestParam String reason) {
        VehicleDto vehicle = vehicleService.rejectVehicle(id, reason);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> activateVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.activateVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> deactivateVehicle(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.deactivateVehicle(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/mark-sold")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<VehicleDto> markAsSold(@PathVariable Long id) {
        VehicleDto vehicle = vehicleService.markAsSold(id);
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VehicleDto>> getPendingApprovalVehicles() {
        List<VehicleDto> vehicles = vehicleService.getPendingApprovalVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VehicleDto>> getVehiclesByStatus(@PathVariable VehicleStatus status) {
        List<VehicleDto> vehicles = vehicleService.getVehiclesByStatus(status);
        return ResponseEntity.ok(vehicles);
    }
}
