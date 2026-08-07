package swari.sewa.module.analytics.engine.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.analytics.engine.BusinessCalculationEngine;
import swari.sewa.module.analytics.util.ChartDataUtil;
import swari.sewa.module.analytics.util.DateFilterUtil;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BusinessCalculationEngineImpl implements BusinessCalculationEngine {

    private final VehicleRepository vehicleRepository;
    private final ExpenseRepository expenseRepository;

    // ==================== STOCK KPIS (Point-in-Time) ====================
    // NOTE: Inventory ownership is separate from website publishing.
    // INACTIVE status means "not published on website", NOT "absent from inventory".
    // All owned vehicles (ACTIVE, INACTIVE, PENDING_SALE) are counted in stock until sold.

    @Override
    public Long getCurrentStock(Long shopId) {
        // Current Stock = All vehicles owned by showroom (except SOLD)
        // Includes: ACTIVE (published), INACTIVE (unpublished), PENDING_SALE (reserved)
        Long count = vehicleRepository.countByShopIdAndStatusIn(shopId,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE));
        log.debug("getCurrentStock - shopId: {}, count: {}", shopId, count);
        return count;
    }

    @Override
    public Long getAvailableStock(Long shopId) {
        // Available Stock = Vehicles that can be sold immediately
        // Includes: ACTIVE (published), INACTIVE (unpublished but can be sold from showroom)
        // Excludes: PENDING_SALE (reserved for customers)
        Long count = vehicleRepository.countByShopIdAndStatusIn(shopId,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE));
        log.debug("getAvailableStock - shopId: {}, count: {}", shopId, count);
        return count;
    }

    @Override
    public Long getReservedStock(Long shopId) {
        // Reserved Stock = Vehicles reserved for customers (PENDING_SALE)
        // These are still in inventory but not available for new sales
        Long count = vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.PENDING_SALE);
        log.debug("getReservedStock - shopId: {}, count: {}", shopId, count);
        return count;
    }

    @Override
    public BigDecimal getInventoryValue(Long shopId) {
        // Inventory Value = Total investment in vehicles currently owned by showroom
        // Includes: ACTIVE (published), INACTIVE (unpublished), PENDING_SALE (reserved)
        // Excludes: SOLD (no longer owned)
        BigDecimal value = vehicleRepository.sumPurchasePriceByShopIdAndStatusIn(shopId,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE));
        log.debug("getInventoryValue - shopId: {}, value: {}", shopId, value);
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==================== PERIOD KPIS (Date Filtered) ====================

    @Override
    public Long getVehiclesPurchased(Long shopId, LocalDate startDate, LocalDate endDate) {
        Long count = vehicleRepository.countByShopIdAndBoughtDateBetween(shopId, startDate, endDate);
        log.debug("getVehiclesPurchased - shopId: {}, startDate: {}, endDate: {}, count: {}", shopId, startDate, endDate, count);
        return count != null ? count : 0L;
    }

    @Override
    public Long getVehiclesSold(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        Long count = vehicleRepository.countByShopIdAndStatusAndSoldAtBetween(shopId, 
            VehicleStatus.SOLD, startDate, endDate);
        return count != null ? count : 0L;
    }

    @Override
    public BigDecimal getSalesValue(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        // Sales Value = Total revenue from vehicle sales (sum of selling prices)
        BigDecimal value = vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(shopId,
            VehicleStatus.SOLD, startDate, endDate);
        log.debug("getSalesValue - shopId: {}, startDate: {}, endDate: {}, value: {}", shopId, startDate, endDate, value);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getInventoryPurchased(Long shopId, LocalDate startDate, LocalDate endDate) {
        BigDecimal value = vehicleRepository.sumPurchasePriceByShopIdAndBoughtDateBetween(shopId, startDate, endDate);
        log.debug("getInventoryPurchased - shopId: {}, startDate: {}, endDate: {}, value: {}", shopId, startDate, endDate, value);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getOperatingExpenses(Long shopId, LocalDate startDate, LocalDate endDate) {
        BigDecimal value = expenseRepository.sumAmountByShopIdAndExpenseDateBetween(shopId, startDate, endDate);
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==================== PROFITABILITY KPIS ====================

    @Override
    public BigDecimal getCOGS(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        // COGS = Purchase Price + Repair Cost + Additional Expenses for sold vehicles
        BigDecimal value = vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(shopId, 
            VehicleStatus.SOLD, startDate, endDate);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getGrossProfit(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal salesValue = getSalesValue(shopId, startDate, endDate);
        BigDecimal cogs = getCOGS(shopId, startDate, endDate);
        return salesValue.subtract(cogs);
    }

    @Override
    public BigDecimal getNetProfit(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal grossProfit = getGrossProfit(shopId, startDate, endDate);
        BigDecimal operatingExpenses = getOperatingExpenses(shopId, startDate.toLocalDate(), endDate.toLocalDate());
        return grossProfit.subtract(operatingExpenses);
    }

    // ==================== AVERAGE KPIS ====================

    @Override
    public BigDecimal getAveragePurchasePrice(Long shopId, LocalDate startDate, LocalDate endDate) {
        Long vehiclesPurchased = getVehiclesPurchased(shopId, startDate, endDate);
        if (vehiclesPurchased == null || vehiclesPurchased == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal inventoryPurchased = getInventoryPurchased(shopId, startDate, endDate);
        return inventoryPurchased.divide(BigDecimal.valueOf(vehiclesPurchased), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getAverageSellingPrice(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        Long vehiclesSold = getVehiclesSold(shopId, startDate, endDate);
        if (vehiclesSold == null || vehiclesSold == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal salesValue = getSalesValue(shopId, startDate, endDate);
        return salesValue.divide(BigDecimal.valueOf(vehiclesSold), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getAverageProfitPerVehicle(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        Long vehiclesSold = getVehiclesSold(shopId, startDate, endDate);
        if (vehiclesSold == null || vehiclesSold == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal grossProfit = getGrossProfit(shopId, startDate, endDate);
        return grossProfit.divide(BigDecimal.valueOf(vehiclesSold), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getProfitMargin(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal salesValue = getSalesValue(shopId, startDate, endDate);
        if (salesValue == null || salesValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal grossProfit = getGrossProfit(shopId, startDate, endDate);
        return grossProfit.divide(salesValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    // ==================== INVENTORY ANALYSIS KPIS ====================

    @Override
    public BigDecimal getInventoryTurnover(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal cogs = getCOGS(shopId, startDate, endDate);
        BigDecimal currentInventoryValue = getInventoryValue(shopId);
        
        if (currentInventoryValue == null || currentInventoryValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Simplified: using current inventory as average
        // In production, should calculate average of beginning and ending inventory
        return cogs.divide(currentInventoryValue, 4, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getDaysInInventory(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal inventoryTurnover = getInventoryTurnover(shopId, startDate, endDate);
        if (inventoryTurnover == null || inventoryTurnover.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(365).divide(inventoryTurnover, 2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, Long> getStockAgeAnalysis(Long shopId) {
        Map<String, Long> ageAnalysis = new HashMap<>();
        LocalDate today = LocalDate.now();

        // Stock age analysis includes all owned vehicles (ACTIVE, INACTIVE, PENDING_SALE)
        // INACTIVE vehicles are physically in inventory even if not published on website

        // 0-30 days
        LocalDate thirtyDaysAgo = today.minusDays(30);
        Long age0to30 = vehicleRepository.countByShopIdAndBoughtDateAfterAndStatusIn(
            shopId, thirtyDaysAgo,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE)
        );
        ageAnalysis.put("0-30", age0to30 != null ? age0to30 : 0L);

        // 31-60 days
        LocalDate sixtyDaysAgo = today.minusDays(60);
        Long age31to60 = vehicleRepository.countByShopIdAndBoughtDateBetweenAndStatusIn(
            shopId, sixtyDaysAgo, thirtyDaysAgo,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE)
        );
        ageAnalysis.put("31-60", age31to60 != null ? age31to60 : 0L);

        // 61-90 days
        LocalDate ninetyDaysAgo = today.minusDays(90);
        Long age61to90 = vehicleRepository.countByShopIdAndBoughtDateBetweenAndStatusIn(
            shopId, ninetyDaysAgo, sixtyDaysAgo,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE)
        );
        ageAnalysis.put("61-90", age61to90 != null ? age61to90 : 0L);

        // 90+ days
        Long age90Plus = vehicleRepository.countByShopIdAndBoughtDateBeforeAndStatusIn(
            shopId, ninetyDaysAgo,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE)
        );
        ageAnalysis.put("90+", age90Plus != null ? age90Plus : 0L);

        return ageAnalysis;
    }

    @Override
    public BigDecimal getDeadStockValue(Long shopId) {
        // Dead Stock = Vehicles in inventory for 90+ days (not sold yet)
        // Includes: ACTIVE, INACTIVE, PENDING_SALE (all owned vehicles, regardless of website visibility)
        LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
        BigDecimal value = vehicleRepository.sumPurchasePriceByShopIdAndBoughtDateBeforeAndStatusIn(
            shopId, ninetyDaysAgo,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE)
        );
        log.debug("getDeadStockValue - shopId: {}, value: {}", shopId, value);
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==================== FINANCIAL KPIS (Finance Module) ====================

    @Override
    public CashFlowData getCashFlow(Long shopId, LocalDateTime startDate, LocalDateTime endDate) {
        // Money In - Bike Sales
        BigDecimal moneyIn = getSalesValue(shopId, startDate, endDate);
        
        // Money Out - Vehicle Purchases + Expenses
        BigDecimal moneyOutVehiclePurchases = getInventoryPurchased(shopId, startDate.toLocalDate(), endDate.toLocalDate());
        BigDecimal moneyOutExpenses = getOperatingExpenses(shopId, startDate.toLocalDate(), endDate.toLocalDate());
        BigDecimal moneyOut = moneyOutVehiclePurchases.add(moneyOutExpenses);
        
        // Net Cash Flow
        BigDecimal netCashFlow = moneyIn.subtract(moneyOut);
        
        return new CashFlowData(moneyIn, moneyOut, netCashFlow);
    }

    @Override
    public VehicleInvestmentData getVehicleInvestment(Long shopId) {
        // Total investment = sum of purchase prices of all vehicles ever bought
        BigDecimal totalInvestment = vehicleRepository.sumPurchasePriceByShopId(shopId);
        
        // Current inventory cost (at purchase price)
        BigDecimal currentInventoryCost = getInventoryValue(shopId);
        
        // Current inventory selling value (at current selling prices)
        BigDecimal currentInventorySellingValue = vehicleRepository.sumPriceByShopIdAndStatusIn(shopId,
            List.of(VehicleStatus.ACTIVE, VehicleStatus.INACTIVE, VehicleStatus.PENDING_SALE));
        
        // Vehicles sold value (at purchase price)
        BigDecimal vehiclesSoldValue = vehicleRepository.sumPurchasePriceByShopIdAndStatus(shopId, "SOLD");
        
        // Expected profit from current inventory
        BigDecimal expectedProfit = currentInventorySellingValue.subtract(currentInventoryCost);
        
        // Expected margin percentage
        BigDecimal expectedMargin = BigDecimal.ZERO;
        if (currentInventorySellingValue.compareTo(BigDecimal.ZERO) > 0) {
            expectedMargin = expectedProfit.divide(currentInventorySellingValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        
        // ROI calculation
        BigDecimal roi = BigDecimal.ZERO;
        if (totalInvestment != null && totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalProfit = currentInventorySellingValue.subtract(currentInventoryCost != null ? currentInventoryCost : BigDecimal.ZERO);
            roi = totalProfit.divide(totalInvestment, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        
        // Unsold investment = current inventory cost
        BigDecimal unsoldInvestment = currentInventoryCost;
        
        // Average cost price
        Long totalVehicles = vehicleRepository.countByShopId(shopId);
        BigDecimal averageCostPrice = BigDecimal.ZERO;
        if (totalVehicles != null && totalVehicles > 0 && totalInvestment != null) {
            averageCostPrice = totalInvestment.divide(BigDecimal.valueOf(totalVehicles), 2, RoundingMode.HALF_UP);
        }
        
        // Average selling price (of current inventory)
        Long currentVehicleCount = getCurrentStock(shopId);
        BigDecimal averageSellingPrice = BigDecimal.ZERO;
        if (currentVehicleCount != null && currentVehicleCount > 0 && currentInventorySellingValue != null) {
            averageSellingPrice = currentInventorySellingValue.divide(BigDecimal.valueOf(currentVehicleCount), 2, RoundingMode.HALF_UP);
        }
        
        return new VehicleInvestmentData(
            totalInvestment != null ? totalInvestment : BigDecimal.ZERO,
            currentInventoryCost,
            currentInventorySellingValue != null ? currentInventorySellingValue : BigDecimal.ZERO,
            vehiclesSoldValue != null ? vehiclesSoldValue : BigDecimal.ZERO,
            expectedProfit,
            expectedMargin,
            roi,
            unsoldInvestment,
            averageCostPrice,
            averageSellingPrice
        );
    }

    @Override
    public List<ProfitTrendData> getProfitTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly) {
        List<Object[]> profitTrendData = vehicleRepository.getProfitTrend(shopId, startDate, endDate, isYearly);
        List<Object[]> filledProfitTrendData = ChartDataUtil.fillMissingPeriods(profitTrendData, startDate, endDate, isYearly);
        
        List<ProfitTrendData> result = new ArrayList<>();
        for (Object[] row : filledProfitTrendData) {
            BigDecimal grossProfit = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : (row[1] != null ? new BigDecimal(((Number) row[1]).longValue()) : BigDecimal.ZERO);
            BigDecimal netProfit = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : (row[2] != null ? new BigDecimal(((Number) row[2]).longValue()) : BigDecimal.ZERO);
            result.add(new ProfitTrendData((String) row[0], grossProfit, netProfit));
        }
        
        return result;
    }

    @Override
    public List<RevenueTrendData> getRevenueTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly) {
        List<Object[]> revenueTrendData = vehicleRepository.getSalesTrend(shopId, startDate, endDate, isYearly);
        List<Object[]> filledRevenueTrendData = ChartDataUtil.fillMissingPeriods(revenueTrendData, startDate, endDate, isYearly);
        
        List<RevenueTrendData> result = new ArrayList<>();
        for (Object[] row : filledRevenueTrendData) {
            BigDecimal revenue = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : (row[1] != null ? new BigDecimal(((Number) row[1]).longValue()) : BigDecimal.ZERO);
            result.add(new RevenueTrendData((String) row[0], revenue));
        }
        
        return result;
    }

    @Override
    public List<ExpenseTrendData> getExpenseTrend(Long shopId, LocalDate startDate, LocalDate endDate, boolean isYearly) {
        List<Object[]> expenseTrendData = expenseRepository.getExpenseTrend(shopId, startDate, endDate, isYearly);
        List<Object[]> filledExpenseTrendData = ChartDataUtil.fillMissingPeriods(expenseTrendData, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), isYearly);
        
        List<ExpenseTrendData> result = new ArrayList<>();
        for (Object[] row : filledExpenseTrendData) {
            BigDecimal expenses = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : (row[1] != null ? new BigDecimal(((Number) row[1]).longValue()) : BigDecimal.ZERO);
            result.add(new ExpenseTrendData((String) row[0], expenses));
        }
        
        return result;
    }

    @Override
    public List<CashFlowTrendData> getCashFlowTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly) {
        List<Object[]> cashFlowTrendData = vehicleRepository.getCashFlowTrend(shopId, startDate, endDate, isYearly);
        List<Object[]> filledCashFlowTrendData = ChartDataUtil.fillMissingPeriodsThreeValues(cashFlowTrendData, startDate, endDate, isYearly);
        
        List<CashFlowTrendData> result = new ArrayList<>();
        for (Object[] row : filledCashFlowTrendData) {
            BigDecimal moneyIn = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : (row[1] != null ? new BigDecimal(((Number) row[1]).longValue()) : BigDecimal.ZERO);
            BigDecimal moneyOut = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : (row[2] != null ? new BigDecimal(((Number) row[2]).longValue()) : BigDecimal.ZERO);
            BigDecimal netCashFlow = moneyIn.subtract(moneyOut);
            result.add(new CashFlowTrendData((String) row[0], moneyIn, moneyOut, netCashFlow));
        }
        
        return result;
    }

    @Override
    public OutstandingData getOutstanding(Long shopId) {
        // Get customer pending (from vehicles with PENDING_SALE status)
        BigDecimal customerPending = vehicleRepository.sumPriceByShopIdAndStatus(shopId, VehicleStatus.PENDING_SALE);
        
        // Get finance pending (simplified - would need finance company tracking table)
        BigDecimal financePending = BigDecimal.ZERO;
        
        // Get supplier pending (simplified - would need supplier tracking table)
        BigDecimal supplierPending = BigDecimal.ZERO;
        
        BigDecimal totalReceivable = (customerPending != null ? customerPending : BigDecimal.ZERO).add(financePending);
        BigDecimal totalPayable = supplierPending != null ? supplierPending : BigDecimal.ZERO;
        BigDecimal netOutstanding = totalReceivable.subtract(totalPayable);
        
        // Build receivables list
        List<ReceivableItem> receivables = new ArrayList<>();
        if (customerPending != null && customerPending.compareTo(BigDecimal.ZERO) > 0) {
            receivables.add(new ReceivableItem("Customer", "Multiple Customers", customerPending));
        }
        
        // Build payables list
        List<PayableItem> payables = new ArrayList<>();
        // Placeholder for supplier payables
        
        return new OutstandingData(totalReceivable, totalPayable, netOutstanding, receivables, payables);
    }

    @Override
    public PaymentSummaryData getPaymentSummary(Long shopId, LocalDate startDate, LocalDate endDate) {
        // Calculate received today
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();
        BigDecimal receivedToday = getSalesValue(shopId, todayStart, todayEnd);
        
        // Calculate received this month
        LocalDateTime monthStart = startDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = endDate.atTime(23, 59, 59);
        BigDecimal receivedThisMonth = getSalesValue(shopId, monthStart, monthEnd);
        
        // Pending customer payments (PENDING_SALE vehicles)
        BigDecimal pendingCustomerPayments = vehicleRepository.sumPriceByShopIdAndStatus(shopId, VehicleStatus.PENDING_SALE);
        
        // Supplier payables (simplified - would need supplier tracking)
        BigDecimal supplierPayables = BigDecimal.ZERO;
        
        // Finance company receivables (simplified - would need finance tracking)
        BigDecimal financeCompanyReceivables = BigDecimal.ZERO;
        
        return new PaymentSummaryData(
            receivedToday,
            receivedThisMonth,
            pendingCustomerPayments != null ? pendingCustomerPayments : BigDecimal.ZERO,
            supplierPayables,
            financeCompanyReceivables
        );
    }
}
