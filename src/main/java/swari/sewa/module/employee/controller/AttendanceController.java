package swari.sewa.module.employee.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import swari.sewa.module.employee.dto.AttendanceDto;
import swari.sewa.module.employee.dto.AttendanceRequestDto;
import swari.sewa.module.employee.service.AttendanceService;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    
    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#requestDto.employeeId, authentication.name)")
    public ResponseEntity<AttendanceDto> createAttendance(@Valid @RequestBody AttendanceRequestDto requestDto) {
        AttendanceDto attendance = attendanceService.createAttendance(requestDto);
        return ResponseEntity.ok(attendance);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAttendanceOwner(#id, authentication.name)")
    public ResponseEntity<AttendanceDto> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceDto attendanceDto) {
        AttendanceDto updatedAttendance = attendanceService.updateAttendance(id, attendanceDto);
        return ResponseEntity.ok(updatedAttendance);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAttendanceOwner(#id, authentication.name)")
    public ResponseEntity<AttendanceDto> getAttendanceById(@PathVariable Long id) {
        return attendanceService.getAttendanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isAttendanceOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByEmployee(@PathVariable Long employeeId) {
        List<AttendanceDto> attendance = attendanceService.getAttendanceByEmployee(employeeId);
        return ResponseEntity.ok(attendance);
    }
    
    @GetMapping("/shop/{shopId}/date/{date}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByDate(
            @PathVariable Long shopId,
            @PathVariable LocalDate date) {
        List<AttendanceDto> attendance = attendanceService.getAttendanceByDateAndShopId(date, shopId);
        return ResponseEntity.ok(attendance);
    }
    
    @GetMapping("/shop/{shopId}/month/{year}/{month}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByMonth(
            @PathVariable Long shopId,
            @PathVariable int year,
            @PathVariable int month) {
        List<AttendanceDto> attendance = attendanceService.getAttendanceByMonth(year, month, shopId);
        return ResponseEntity.ok(attendance);
    }
    
    @GetMapping("/employee/{employeeId}/month/{year}/{month}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByEmployeeAndMonth(
            @PathVariable Long employeeId,
            @PathVariable int year,
            @PathVariable int month) {
        List<AttendanceDto> attendance = attendanceService.getAttendanceByEmployeeAndMonth(employeeId, year, month);
        return ResponseEntity.ok(attendance);
    }
    
    @GetMapping("/employee/{employeeId}/date/{date}")
    @PreAuthorize("hasRole('SHOP_OWNER') and @employeeSecurity.isEmployeeOwner(#employeeId, authentication.name)")
    public ResponseEntity<AttendanceDto> getAttendanceByEmployeeAndDate(
            @PathVariable Long employeeId,
            @PathVariable LocalDate date) {
        return attendanceService.getAttendanceByEmployeeAndDate(employeeId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/shop/{shopId}/bulk")
    @PreAuthorize("hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#shopId, authentication.name)")
    public ResponseEntity<List<AttendanceDto>> bulkUpdateAttendance(
            @PathVariable Long shopId,
            @RequestBody List<AttendanceRequestDto> requests) {
        List<AttendanceDto> result = attendanceService.bulkUpdateAttendance(shopId, requests);
        return ResponseEntity.ok(result);
    }
}
