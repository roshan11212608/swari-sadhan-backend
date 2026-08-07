package swari.sewa.module.finance.service;

import swari.sewa.module.finance.dto.*;

public interface FinanceService {
    FinancialDashboardResponse getFinancialDashboard(Long shopId, String filter);
    IncomeResponse getIncome(Long shopId, String filter);
    FinanceExpensesResponse getFinanceExpenses(Long shopId, String filter);
    ProfitResponse getProfit(Long shopId, String filter);
    CashFlowResponse getCashFlow(Long shopId, String filter);
    OutstandingResponse getOutstanding(Long shopId);
    VehicleInvestmentResponse getVehicleInvestment(Long shopId);
    PaymentSummaryResponse getPaymentSummary(Long shopId);
}
