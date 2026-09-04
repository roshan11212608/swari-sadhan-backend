package swari.sewa.module.employee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.LeaveRequestRepository;
import swari.sewa.module.employee.service.impl.AdvancePaymentServiceImpl;
import swari.sewa.module.employee.service.impl.LeaveServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression tests for Leave and Advance summary endpoints.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getLeaveSummary uses a single GROUP BY query, not findByShopIdOrderByAppliedDateDesc</li>
 *   <li>getLeaveSummary returns correct counts per status plus a Total</li>
 *   <li>getLeaveSummary with no data returns zero counts</li>
 *   <li>getAdvanceSummary uses aggregation queries, not findActiveByShopId or findByEmployeeId</li>
 *   <li>getAdvanceSummary returns correct totals and active count</li>
 *   <li>getAdvanceSummary with no data returns zero values</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class LeaveAndAdvanceSummaryPerformanceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;

    @Mock private AdvancePaymentRepository advancePaymentRepository;

    @InjectMocks private LeaveServiceImpl leaveService;

    @InjectMocks private AdvancePaymentServiceImpl advancePaymentService;

    // ── Leave Summary ──

    @Test
    void getLeaveSummary_usesGroupByQuery_notLoadAll() {
        Long shopId = 1L;
        when(leaveRequestRepository.countByShopIdGroupByStatus(shopId))
                .thenReturn(List.of(
                        new Object[]{"Pending", 5L},
                        new Object[]{"Approved", 10L},
                        new Object[]{"Rejected", 3L}
                ));

        Map<String, Long> summary = leaveService.getLeaveSummary(shopId);

        assertNotNull(summary);
        assertEquals(5L, summary.get("Pending"));
        assertEquals(10L, summary.get("Approved"));
        assertEquals(3L, summary.get("Rejected"));
        assertEquals(18L, summary.get("Total"));

        // Verify GROUP BY query was used
        verify(leaveRequestRepository).countByShopIdGroupByStatus(shopId);
        // Verify full-table load was NOT called
        verify(leaveRequestRepository, never()).findByShopIdOrderByAppliedDateDesc(eq(shopId));
        verify(leaveRequestRepository, never()).findPendingByShopId(eq(shopId));
    }

    @Test
    void getLeaveSummary_withNoData_returnsZeroTotal() {
        Long shopId = 1L;
        when(leaveRequestRepository.countByShopIdGroupByStatus(shopId))
                .thenReturn(List.of());

        Map<String, Long> summary = leaveService.getLeaveSummary(shopId);

        assertNotNull(summary);
        assertEquals(0L, summary.get("Total"));
        // No status keys should be present (only Total)
        assertEquals(1, summary.size());

        verify(leaveRequestRepository).countByShopIdGroupByStatus(shopId);
    }

    @Test
    void getLeaveSummary_withCancelledStatus_includesCancelledInTotal() {
        Long shopId = 1L;
        when(leaveRequestRepository.countByShopIdGroupByStatus(shopId))
                .thenReturn(List.of(
                        new Object[]{"Pending", 2L},
                        new Object[]{"Cancelled", 1L}
                ));

        Map<String, Long> summary = leaveService.getLeaveSummary(shopId);

        assertEquals(2L, summary.get("Pending"));
        assertEquals(1L, summary.get("Cancelled"));
        assertEquals(3L, summary.get("Total"));

        verify(leaveRequestRepository).countByShopIdGroupByStatus(shopId);
    }

    // ── Advance Summary ──

    @Test
    void getAdvanceSummary_usesAggregationQueries_notLoadAll() {
        Long shopId = 1L;
        when(advancePaymentRepository.sumAdvanceAmountByShopId(shopId))
                .thenReturn(new BigDecimal("50000"));
        when(advancePaymentRepository.sumRecoveredAmountByShopId(shopId))
                .thenReturn(new BigDecimal("20000"));
        when(advancePaymentRepository.sumRemainingBalanceByShopId(shopId))
                .thenReturn(new BigDecimal("15000"));
        when(advancePaymentRepository.countByShopIdAndStatus(shopId, "Pending"))
                .thenReturn(5L);

        Map<String, Object> summary = advancePaymentService.getAdvanceSummary(shopId);

        assertNotNull(summary);
        assertEquals(new BigDecimal("50000"), summary.get("totalAdvanceGiven"));
        assertEquals(new BigDecimal("20000"), summary.get("recoveredAmount"));
        assertEquals(new BigDecimal("15000"), summary.get("pendingRecovery"));
        assertEquals(5L, summary.get("activeRequests"));

        // Verify aggregation queries were used
        verify(advancePaymentRepository).sumAdvanceAmountByShopId(shopId);
        verify(advancePaymentRepository).sumRecoveredAmountByShopId(shopId);
        verify(advancePaymentRepository).sumRemainingBalanceByShopId(shopId);
        verify(advancePaymentRepository).countByShopIdAndStatus(shopId, "Pending");
        // Verify full-table load was NOT called
        verify(advancePaymentRepository, never()).findActiveByShopId(eq(shopId));
        verify(advancePaymentRepository, never()).findPendingByShopId(eq(shopId));
    }

    @Test
    void getAdvanceSummary_withNoData_returnsZeros() {
        Long shopId = 1L;
        when(advancePaymentRepository.sumAdvanceAmountByShopId(shopId)).thenReturn(null);
        when(advancePaymentRepository.sumRecoveredAmountByShopId(shopId)).thenReturn(null);
        when(advancePaymentRepository.sumRemainingBalanceByShopId(shopId)).thenReturn(null);
        when(advancePaymentRepository.countByShopIdAndStatus(shopId, "Pending")).thenReturn(null);

        Map<String, Object> summary = advancePaymentService.getAdvanceSummary(shopId);

        assertNotNull(summary);
        assertEquals(BigDecimal.ZERO, summary.get("totalAdvanceGiven"));
        assertEquals(BigDecimal.ZERO, summary.get("recoveredAmount"));
        assertEquals(BigDecimal.ZERO, summary.get("pendingRecovery"));
        assertEquals(0L, summary.get("activeRequests"));

        verify(advancePaymentRepository).sumAdvanceAmountByShopId(shopId);
        verify(advancePaymentRepository).sumRecoveredAmountByShopId(shopId);
        verify(advancePaymentRepository).sumRemainingBalanceByShopId(shopId);
        verify(advancePaymentRepository).countByShopIdAndStatus(shopId, "Pending");
    }
}
