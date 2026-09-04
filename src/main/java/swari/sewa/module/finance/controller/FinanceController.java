package swari.sewa.module.finance.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.finance.dto.*;
import swari.sewa.module.finance.service.FinanceService;
import swari.sewa.module.shop.repository.ShopRepository;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FinanceController {

    private final FinanceService financeService;
    private final ShopRepository shopRepository;

    /** Resolve shop ID by user email, falling back to shop owner email.
     *  This handles the race condition where the shop was auto-created by the
     *  profile endpoint but the user link may not be set yet, as well as shops
     *  that were created without a user link. */
    private Long resolveShopId(String userEmail) {
        return shopRepository.findShopIdByUserEmail(userEmail)
                .or(() -> shopRepository.findShopIdByShopOwnerEmail(userEmail))
                .orElseThrow(() -> new RuntimeException("Shop not found for user: " + userEmail));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeDashboard", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<FinancialDashboardResponse>> getFinancialDashboard(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        FinancialDashboardResponse dashboard = financeService.getFinancialDashboard(shopId, filter);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Financial dashboard loaded successfully"));
    }

    @GetMapping("/income")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeIncome", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<IncomeResponse>> getIncome(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        IncomeResponse income = financeService.getIncome(shopId, filter);
        return ResponseEntity.ok(ApiResponse.success(income, "Income data loaded successfully"));
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeExpenses", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<FinanceExpensesResponse>> getFinanceExpenses(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        FinanceExpensesResponse expenses = financeService.getFinanceExpenses(shopId, filter);
        return ResponseEntity.ok(ApiResponse.success(expenses, "Expenses data loaded successfully"));
    }

    @GetMapping("/profit")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeProfit", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<ProfitResponse>> getProfit(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        ProfitResponse profit = financeService.getProfit(shopId, filter);
        return ResponseEntity.ok(ApiResponse.success(profit, "Profit data loaded successfully"));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeCashFlow", key = "#authentication.name + '_' + #filter", unless = "#result == null")
    public ResponseEntity<ApiResponse<CashFlowResponse>> getCashFlow(
            @RequestParam String filter,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        CashFlowResponse cashFlow = financeService.getCashFlow(shopId, filter);
        return ResponseEntity.ok(ApiResponse.success(cashFlow, "Cash flow data loaded successfully"));
    }

    @GetMapping("/outstanding")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "financeOutstanding", key = "#authentication.name", unless = "#result == null")
    public ResponseEntity<ApiResponse<OutstandingResponse>> getOutstanding(
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        OutstandingResponse outstanding = financeService.getOutstanding(shopId);
        return ResponseEntity.ok(ApiResponse.success(outstanding, "Outstanding data loaded successfully"));
    }

    @GetMapping("/vehicle-investment")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "vehicleInvestment", key = "#authentication.name", unless = "#result == null")
    public ResponseEntity<ApiResponse<VehicleInvestmentResponse>> getVehicleInvestment(
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        VehicleInvestmentResponse investment = financeService.getVehicleInvestment(shopId);
        return ResponseEntity.ok(ApiResponse.success(investment, "Vehicle investment data loaded successfully"));
    }

    @GetMapping("/payment-summary")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Cacheable(value = "paymentSummary", key = "#authentication.name", unless = "#result == null")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            Authentication authentication) {
        String userEmail = authentication.getName();
        Long shopId = resolveShopId(userEmail);

        PaymentSummaryResponse paymentSummary = financeService.getPaymentSummary(shopId);
        return ResponseEntity.ok(ApiResponse.success(paymentSummary, "Payment summary data loaded successfully"));
    }

    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary"}, allEntries = true)
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Void>> clearFinanceCache(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(null, "Finance cache cleared successfully"));
    }
}
