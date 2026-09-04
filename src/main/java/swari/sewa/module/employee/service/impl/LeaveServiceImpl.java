package swari.sewa.module.employee.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.dto.LeaveRequestRequestDto;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.entity.LeaveRequest;
import swari.sewa.module.employee.mapper.LeaveRequestMapper;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.repository.LeaveRequestRepository;
import swari.sewa.module.employee.service.LeaveService;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {
    
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    
    @Override
    public LeaveRequestDto createLeaveRequest(LeaveRequestRequestDto requestDto) {
        Employee employee = employeeRepository.findById(requestDto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + requestDto.getEmployeeId()));

        // Validate end date is not before start date
        if (requestDto.getEndDate().isBefore(requestDto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        // Check for overlapping approved leave requests
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findByEmployeeIdAndStatus(requestDto.getEmployeeId(), "Approved");
        for (LeaveRequest existingLeave : overlappingLeaves) {
            if (!(requestDto.getEndDate().isBefore(existingLeave.getStartDate()) ||
                  requestDto.getStartDate().isAfter(existingLeave.getEndDate()))) {
                throw new IllegalArgumentException("Employee has an approved leave request overlapping with this date range");
            }
        }

        LeaveRequest leaveRequest = leaveRequestMapper.toEntity(requestDto);
        leaveRequest.setEmployee(employee);
        leaveRequest.setEmployeeName(employee.getFullName());
        leaveRequest.setEmployeePhotoUrl(employee.getProfilePhotoUrl());
        leaveRequest.setDesignation(employee.getDesignation());
        leaveRequest.setDepartment(employee.getDepartment());
        leaveRequest.setStatus("Pending");

        // Calculate duration
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(requestDto.getStartDate(), requestDto.getEndDate()) + 1;
        leaveRequest.setDuration((int) daysBetween);

        
        LeaveRequest savedLeave = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toDto(savedLeave);
    }
    
    @Override
    public LeaveRequestDto approveLeave(Long leaveId, String approvedBy) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));
        
        if (!"Pending".equals(leaveRequest.getStatus())) {
            throw new IllegalArgumentException("Leave request is not in pending status");
        }
        
        leaveRequest.setStatus("Approved");
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedDate(LocalDate.now());
        
        LeaveRequest updatedLeave = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toDto(updatedLeave);
    }
    
    @Override
    public LeaveRequestDto rejectLeave(Long leaveId, String rejectionReason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));
        
        if (!"Pending".equals(leaveRequest.getStatus())) {
            throw new IllegalArgumentException("Leave request is not in pending status");
        }
        
        leaveRequest.setStatus("Rejected");
        leaveRequest.setRejectionReason(rejectionReason);
        
        LeaveRequest updatedLeave = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toDto(updatedLeave);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<LeaveRequestDto> getLeaveById(Long id) {
        return leaveRequestRepository.findById(id)
                .map(leaveRequestMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getLeaveByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(leaveRequestMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getPendingLeaves(Long shopId) {
        return leaveRequestRepository.findPendingByShopId(shopId).stream()
                .map(leaveRequestMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getLeavesByStatus(Long shopId, String status) {
        return leaveRequestRepository.findByShopIdAndStatus(shopId, status).stream()
                .map(leaveRequestMapper::toDto)
                .toList();
    }
    
    @Override
    public void calculateLeaveDuration(LeaveRequestRequestDto requestDto) {
        if (requestDto.getStartDate() == null || requestDto.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        
        if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        long duration = ChronoUnit.DAYS.between(requestDto.getStartDate(), requestDto.getEndDate()) + 1;
        // Duration is set in the mapper/entity, not in the DTO
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getLeaveSummary(Long shopId) {
        List<Object[]> rows = leaveRequestRepository.countByShopIdGroupByStatus(shopId);
        Map<String, Long> summary = new HashMap<>();
        long total = 0;
        for (Object[] row : rows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            summary.put(status, count);
            total += count;
        }
        summary.put("Total", total);
        return summary;
    }
}
