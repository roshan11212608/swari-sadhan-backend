package swari.sewa.module.employee.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import swari.sewa.module.employee.dto.AttendanceDto;
import swari.sewa.module.employee.dto.AttendanceRequestDto;

public interface AttendanceService {
    
    AttendanceDto createAttendance(AttendanceRequestDto requestDto);
    
    AttendanceDto updateAttendance(Long id, AttendanceDto attendanceDto);
    
    Optional<AttendanceDto> getAttendanceById(Long id);
    
    void deleteAttendance(Long id);
    
    List<AttendanceDto> getAttendanceByEmployee(Long employeeId);
    
    List<AttendanceDto> getAttendanceByDate(LocalDate date);
    
    List<AttendanceDto> getAttendanceByDateAndShopId(LocalDate date, Long shopId);
    
    List<AttendanceDto> getAttendanceByMonth(int year, int month, Long shopId);
    
    List<AttendanceDto> getAttendanceByEmployeeAndMonth(Long employeeId, int year, int month);
    
    Optional<AttendanceDto> getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date);

    List<AttendanceDto> bulkUpdateAttendance(Long shopId, List<AttendanceRequestDto> requests);

    void calculateWorkingHours(AttendanceDto attendanceDto);
}
