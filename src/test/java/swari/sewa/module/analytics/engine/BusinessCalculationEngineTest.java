package swari.sewa.module.analytics.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.analytics.engine.impl.BusinessCalculationEngineImpl;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessCalculationEngineTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BusinessCalculationEngineImpl businessCalculationEngine;

    private Long shopId = 1L;
    private LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
    private LocalDateTime endDate = LocalDateTime.of(2026, 1, 31, 23, 59);

    @Test
    void testGetCurrentStock() {
        when(vehicleRepository.countByShopIdAndStatusIn(eq(shopId), anyList())).thenReturn(20L);
        assertEquals(20L, businessCalculationEngine.getCurrentStock(shopId));
    }

    @Test
    void testGetInventoryValue_Null() {
        when(vehicleRepository.sumPurchasePriceByShopIdAndStatusIn(eq(shopId), anyList())).thenReturn(null);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getInventoryValue(shopId));
    }

    @Test
    void testGetGrossProfit() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("500000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        assertEquals(new BigDecimal("150000"), businessCalculationEngine.getGrossProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetGrossProfit_Loss() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("300000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        assertEquals(new BigDecimal("-50000"), businessCalculationEngine.getGrossProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetNetProfit() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("500000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        when(expenseRepository.sumAmountByShopIdAndExpenseDateBetween(eq(shopId), any(LocalDate.class), any(LocalDate.class))).thenReturn(new BigDecimal("100000"));
        assertEquals(new BigDecimal("50000"), businessCalculationEngine.getNetProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetAveragePurchasePrice_NoVehicles() {
        when(vehicleRepository.countByShopIdAndBoughtDateBetween(eq(shopId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getAveragePurchasePrice(shopId, startDate.toLocalDate(), endDate.toLocalDate()));
    }

    @Test
    void testGetProfitMargin_NoSales() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getProfitMargin(shopId, startDate, endDate));
    }
}
