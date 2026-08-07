package swari.sewa.module.employee.service;

import java.util.List;
import java.util.Optional;

import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.dto.LeaveRequestRequestDto;

public interface LeaveService {
    
    LeaveRequestDto createLeaveRequest(LeaveRequestRequestDto requestDto);
    
    LeaveRequestDto approveLeave(Long leaveId, String approvedBy);
    
    LeaveRequestDto rejectLeave(Long leaveId, String rejectionReason);
    
    Optional<LeaveRequestDto> getLeaveById(Long id);
    
    List<LeaveRequestDto> getLeaveByEmployee(Long employeeId);
    
    List<LeaveRequestDto> getPendingLeaves(Long shopId);
    
    List<LeaveRequestDto> getLeavesByStatus(Long shopId, String status);
    
    void calculateLeaveDuration(LeaveRequestRequestDto requestDto);
}
