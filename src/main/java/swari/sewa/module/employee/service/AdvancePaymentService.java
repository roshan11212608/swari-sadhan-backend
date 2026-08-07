package swari.sewa.module.employee.service;

import java.util.List;
import java.util.Optional;

import swari.sewa.module.employee.dto.AdvancePaymentDto;
import swari.sewa.module.employee.dto.AdvancePaymentRequestDto;

public interface AdvancePaymentService {
    
    AdvancePaymentDto createAdvancePayment(AdvancePaymentRequestDto requestDto);
    
    AdvancePaymentDto approveAdvance(Long advanceId, String approvedBy);
    
    AdvancePaymentDto rejectAdvance(Long advanceId, String rejectionReason);
    
    AdvancePaymentDto updateRecovery(Long advanceId, java.math.BigDecimal recoveredAmount);
    
    Optional<AdvancePaymentDto> getAdvanceById(Long id);
    
    List<AdvancePaymentDto> getAdvanceByEmployee(Long employeeId);
    
    List<AdvancePaymentDto> getPendingAdvances(Long shopId);
    
    List<AdvancePaymentDto> getActiveAdvances(Long shopId);
    
    void calculateRecovery(AdvancePaymentDto advanceDto);
}
