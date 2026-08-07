package swari.sewa.module.analytics.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Business Calculation Engine
 * 
 * This is the single source of truth for all business calculations across the entire ERP.
 * Every module (Analytics, Finance, Reports, Mobile, Desktop)
 * must reuse this engine to ensure consistent calculations.
 * 
 * No business formula should exist in multiple places.
 * Services should only orchestrate and compose responses by calling this engine.
 */
public interface BusinessCalculationEngine {

    // ==================== STOCK KPIS (Point-in-Time) ====================
    // These KPIs do NOT respect date filters - they always show current state

    Long getCurrentStock(Long shopId);
    Long getAvailableStock(Long shopId);
    Long getReservedStock(Long shopId);
    BigDecimal getInventoryValue(Long shopId);

    // ==================== PERIOD KPIS (Date Filtered) ====================

    Long getVehiclesPurchased(Long shopId, LocalDate startDate, LocalDate endDate);
    Long getVehiclesSold(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getSalesValue(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getInventoryPurchased(Long shopId, LocalDate startDate, LocalDate endDate);
    BigDecimal getOperatingExpenses(Long shopId, LocalDate startDate, LocalDate endDate);

    // ==================== PROFITABILITY KPIS ====================

    BigDecimal getCOGS(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getGrossProfit(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getNetProfit(Long shopId, LocalDateTime startDate, LocalDateTime endDate);

    // ==================== AVERAGE KPIS ====================

    BigDecimal getAveragePurchasePrice(Long shopId, LocalDate startDate, LocalDate endDate);
    BigDecimal getAverageSellingPrice(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getAverageProfitPerVehicle(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getProfitMargin(Long shopId, LocalDateTime startDate, LocalDateTime endDate);

    // ==================== INVENTORY ANALYSIS KPIS ====================

    BigDecimal getInventoryTurnover(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getDaysInInventory(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
    Map<String, Long> getStockAgeAnalysis(Long shopId);
    BigDecimal getDeadStockValue(Long shopId);

    // ==================== FINANCIAL KPIS (Finance Module) ====================

    /**
     * Get cash flow for a period
     * Returns CashFlowData with moneyIn, moneyOut, and netCashFlow
     */
    CashFlowData getCashFlow(Long shopId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get vehicle investment metrics
     * Returns VehicleInvestmentData with total investment, current value, ROI, etc.
     */
    VehicleInvestmentData getVehicleInvestment(Long shopId);

    /**
     * Get profit trend data for charts
     * Returns list of period-based profit data
     */
    List<ProfitTrendData> getProfitTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly);

    /**
     * Get revenue trend data for charts
     * Returns list of period-based revenue data
     */
    List<RevenueTrendData> getRevenueTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly);

    /**
     * Get expense trend data for charts
     * Returns list of period-based expense data
     */
    List<ExpenseTrendData> getExpenseTrend(Long shopId, LocalDate startDate, LocalDate endDate, boolean isYearly);

    /**
     * Get cash flow trend data for charts
     * Returns list of period-based cash flow data
     */
    List<CashFlowTrendData> getCashFlowTrend(Long shopId, LocalDateTime startDate, LocalDateTime endDate, boolean isYearly);

    /**
     * Get outstanding payments data
     * Returns OutstandingData with receivables and payables
     */
    OutstandingData getOutstanding(Long shopId);

    /**
     * Get payment summary data
     * Returns PaymentSummaryData with receipts and pending payments
     */
    PaymentSummaryData getPaymentSummary(Long shopId, LocalDate startDate, LocalDate endDate);

    // ==================== INNER CLASSES FOR STRUCTURED DATA ====================

    class CashFlowData {
        private BigDecimal moneyIn;
        private BigDecimal moneyOut;
        private BigDecimal netCashFlow;

        public CashFlowData(BigDecimal moneyIn, BigDecimal moneyOut, BigDecimal netCashFlow) {
            this.moneyIn = moneyIn;
            this.moneyOut = moneyOut;
            this.netCashFlow = netCashFlow;
        }

        public BigDecimal getMoneyIn() { return moneyIn; }
        public BigDecimal getMoneyOut() { return moneyOut; }
        public BigDecimal getNetCashFlow() { return netCashFlow; }
    }

    class VehicleInvestmentData {
        private BigDecimal totalInvestment;
        private BigDecimal currentInventoryCost;
        private BigDecimal currentInventorySellingValue;
        private BigDecimal vehiclesSoldValue;
        private BigDecimal expectedProfit;
        private BigDecimal expectedMargin;
        private BigDecimal roi;
        private BigDecimal unsoldInvestment;
        private BigDecimal averageCostPrice;
        private BigDecimal averageSellingPrice;

        public VehicleInvestmentData(BigDecimal totalInvestment, BigDecimal currentInventoryCost,
                                     BigDecimal currentInventorySellingValue, BigDecimal vehiclesSoldValue,
                                     BigDecimal expectedProfit, BigDecimal expectedMargin, BigDecimal roi,
                                     BigDecimal unsoldInvestment, BigDecimal averageCostPrice,
                                     BigDecimal averageSellingPrice) {
            this.totalInvestment = totalInvestment;
            this.currentInventoryCost = currentInventoryCost;
            this.currentInventorySellingValue = currentInventorySellingValue;
            this.vehiclesSoldValue = vehiclesSoldValue;
            this.expectedProfit = expectedProfit;
            this.expectedMargin = expectedMargin;
            this.roi = roi;
            this.unsoldInvestment = unsoldInvestment;
            this.averageCostPrice = averageCostPrice;
            this.averageSellingPrice = averageSellingPrice;
        }

        // Getters
        public BigDecimal getTotalInvestment() { return totalInvestment; }
        public BigDecimal getCurrentInventoryCost() { return currentInventoryCost; }
        public BigDecimal getCurrentInventorySellingValue() { return currentInventorySellingValue; }
        public BigDecimal getVehiclesSoldValue() { return vehiclesSoldValue; }
        public BigDecimal getExpectedProfit() { return expectedProfit; }
        public BigDecimal getExpectedMargin() { return expectedMargin; }
        public BigDecimal getRoi() { return roi; }
        public BigDecimal getUnsoldInvestment() { return unsoldInvestment; }
        public BigDecimal getAverageCostPrice() { return averageCostPrice; }
        public BigDecimal getAverageSellingPrice() { return averageSellingPrice; }
    }

    class ProfitTrendData {
        private String period;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;

        public ProfitTrendData(String period, BigDecimal grossProfit, BigDecimal netProfit) {
            this.period = period;
            this.grossProfit = grossProfit;
            this.netProfit = netProfit;
        }

        public String getPeriod() { return period; }
        public BigDecimal getGrossProfit() { return grossProfit; }
        public BigDecimal getNetProfit() { return netProfit; }
    }

    class RevenueTrendData {
        private String period;
        private BigDecimal revenue;

        public RevenueTrendData(String period, BigDecimal revenue) {
            this.period = period;
            this.revenue = revenue;
        }

        public String getPeriod() { return period; }
        public BigDecimal getRevenue() { return revenue; }
    }

    class ExpenseTrendData {
        private String period;
        private BigDecimal expenses;

        public ExpenseTrendData(String period, BigDecimal expenses) {
            this.period = period;
            this.expenses = expenses;
        }

        public String getPeriod() { return period; }
        public BigDecimal getExpenses() { return expenses; }
    }

    class CashFlowTrendData {
        private String period;
        private BigDecimal moneyIn;
        private BigDecimal moneyOut;
        private BigDecimal netCashFlow;

        public CashFlowTrendData(String period, BigDecimal moneyIn, BigDecimal moneyOut, BigDecimal netCashFlow) {
            this.period = period;
            this.moneyIn = moneyIn;
            this.moneyOut = moneyOut;
            this.netCashFlow = netCashFlow;
        }

        public String getPeriod() { return period; }
        public BigDecimal getMoneyIn() { return moneyIn; }
        public BigDecimal getMoneyOut() { return moneyOut; }
        public BigDecimal getNetCashFlow() { return netCashFlow; }
    }

    class OutstandingData {
        private BigDecimal totalReceivable;
        private BigDecimal totalPayable;
        private BigDecimal netOutstanding;
        private List<ReceivableItem> receivables;
        private List<PayableItem> payables;

        public OutstandingData(BigDecimal totalReceivable, BigDecimal totalPayable, BigDecimal netOutstanding,
                              List<ReceivableItem> receivables, List<PayableItem> payables) {
            this.totalReceivable = totalReceivable;
            this.totalPayable = totalPayable;
            this.netOutstanding = netOutstanding;
            this.receivables = receivables;
            this.payables = payables;
        }

        public BigDecimal getTotalReceivable() { return totalReceivable; }
        public BigDecimal getTotalPayable() { return totalPayable; }
        public BigDecimal getNetOutstanding() { return netOutstanding; }
        public List<ReceivableItem> getReceivables() { return receivables; }
        public List<PayableItem> getPayables() { return payables; }
    }

    class ReceivableItem {
        private String type;
        private String name;
        private BigDecimal amount;

        public ReceivableItem(String type, String name, BigDecimal amount) {
            this.type = type;
            this.name = name;
            this.amount = amount;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public BigDecimal getAmount() { return amount; }
    }

    class PayableItem {
        private String type;
        private String name;
        private BigDecimal amount;

        public PayableItem(String type, String name, BigDecimal amount) {
            this.type = type;
            this.name = name;
            this.amount = amount;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public BigDecimal getAmount() { return amount; }
    }

    class PaymentSummaryData {
        private BigDecimal receivedToday;
        private BigDecimal receivedThisMonth;
        private BigDecimal pendingCustomerPayments;
        private BigDecimal supplierPayables;
        private BigDecimal financeCompanyReceivables;

        public PaymentSummaryData(BigDecimal receivedToday, BigDecimal receivedThisMonth,
                                BigDecimal pendingCustomerPayments, BigDecimal supplierPayables,
                                BigDecimal financeCompanyReceivables) {
            this.receivedToday = receivedToday;
            this.receivedThisMonth = receivedThisMonth;
            this.pendingCustomerPayments = pendingCustomerPayments;
            this.supplierPayables = supplierPayables;
            this.financeCompanyReceivables = financeCompanyReceivables;
        }

        public BigDecimal getReceivedToday() { return receivedToday; }
        public BigDecimal getReceivedThisMonth() { return receivedThisMonth; }
        public BigDecimal getPendingCustomerPayments() { return pendingCustomerPayments; }
        public BigDecimal getSupplierPayables() { return supplierPayables; }
        public BigDecimal getFinanceCompanyReceivables() { return financeCompanyReceivables; }
    }
}
