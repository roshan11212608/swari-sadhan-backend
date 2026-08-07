package swari.sewa.module.employee.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.employee.dto.AttendanceDto;
import swari.sewa.module.employee.dto.AttendanceRequestDto;
import swari.sewa.module.employee.entity.Attendance;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.mapper.AttendanceMapper;
import swari.sewa.module.employee.repository.AttendanceRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.service.AttendanceService;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;
    
    @Override
    public AttendanceDto createAttendance(AttendanceRequestDto requestDto) {
        Employee employee = employeeRepository.findById(requestDto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + requestDto.getEmployeeId()));
        
        // Check if attendance already exists for this employee on this date
        Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeIdAndDate(
                requestDto.getEmployeeId(), requestDto.getDate());
        if (existingAttendance.isPresent()) {
            throw new IllegalArgumentException("Attendance already exists for this employee on this date");
        }
        
        Attendance attendance = attendanceMapper.toEntity(requestDto);
        attendance.setEmployee(employee);
        attendance.setEmployeeName(employee.getFullName());
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        return attendanceMapper.toDto(savedAttendance);
    }
    
    @Override
    public AttendanceDto updateAttendance(Long id, AttendanceDto attendanceDto) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        
        attendanceMapper.updateEntityFromDto(attendanceDto, attendance);
        
        // Recalculate working hours if clock in/out changed
        if (attendance.getClockIn() != null && attendance.getClockOut() != null) {
            calculateWorkingHours(attendance);
        }
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return attendanceMapper.toDto(updatedAttendance);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AttendanceDto> getAttendanceById(Long id) {
        return attendanceRepository.findById(id)
                .map(attendanceMapper::toDto);
    }
    
    @Override
    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        attendanceRepository.delete(attendance);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId).stream()
                .map(attendanceMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date).stream()
                .map(attendanceMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByDateAndShopId(LocalDate date, Long shopId) {
        return attendanceRepository.findByShopIdAndDate(shopId, date).stream()
                .map(attendanceMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByMonth(int year, int month, Long shopId) {
        return attendanceRepository.findByShopIdAndMonth(shopId, year, month).stream()
                .map(attendanceMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByEmployeeAndMonth(Long employeeId, int year, int month) {
        return attendanceRepository.findByEmployeeIdAndMonthAndYear(employeeId, year, month).stream()
                .map(attendanceMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AttendanceDto> getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .map(attendanceMapper::toDto);
    }

    @Override
    @Transactional
    public List<AttendanceDto> bulkUpdateAttendance(Long shopId, List<AttendanceRequestDto> requests) {
        return requests.stream().map(request -> {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

            // Validate employee belongs to the specified shop
            if (!employee.getShop().getId().equals(shopId)) {
                throw new IllegalArgumentException("Employee does not belong to the specified shop");
            }

            // Check if attendance already exists for this employee on this date
            Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeIdAndDate(
                    request.getEmployeeId(), request.getDate());

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
                attendanceMapper.updateEntityFromDto(attendanceMapper.toDto(attendance), attendance);
                attendance.setStatus(request.getStatus());
                if (request.getReason() != null) {
                    attendance.setReason(request.getReason());
                }
            } else {
                attendance = attendanceMapper.toEntity(request);
                attendance.setEmployee(employee);
                attendance.setEmployeeName(employee.getFullName());
            }

            Attendance savedAttendance = attendanceRepository.save(attendance);
            return attendanceMapper.toDto(savedAttendance);
        }).toList();
    }

    @Override
    public void calculateWorkingHours(AttendanceDto attendanceDto) {
        calculateWorkingHours(attendanceMapper.toEntity(attendanceDto));
    }
    
    private void calculateWorkingHours(Attendance attendance) {
        if (attendance.getClockIn() == null || attendance.getClockOut() == null) {
            attendance.setWorkingHours(BigDecimal.ZERO);
            attendance.setOvertime(BigDecimal.ZERO);
            return;
        }
        
        long minutes = ChronoUnit.MINUTES.between(attendance.getClockIn(), attendance.getClockOut());
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        
        attendance.setWorkingHours(hours);
        
        // Calculate overtime (hours beyond 9)
        BigDecimal standardHours = BigDecimal.valueOf(9);
        if (hours.compareTo(standardHours) > 0) {
            attendance.setOvertime(hours.subtract(standardHours));
        } else {
            attendance.setOvertime(BigDecimal.ZERO);
        }
    }
}
