package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionDashboardResponse {
    private Long totalPlans;
    private Long activePlans;
    private Long draftPlans;
    private Long totalSubscribers;
    private Long activeTrials;

    /**
     * @deprecated ambiguous name — this is cash COLLECTED in the current
     * calendar month, not monthly recurring revenue. Use
     * {@link #currentMonthRevenue} for collections and {@link #mrr} for MRR.
     * Retained for backward compatibility with existing clients.
     */
    @Deprecated
    private BigDecimal monthlyRevenue;

    /**
     * @deprecated ambiguous name — see {@link #currentYearRevenue}.
     */
    @Deprecated
    private BigDecimal yearlyRevenue;

    /** Cash collected from COMPLETED transactions in the current calendar month. */
    private BigDecimal currentMonthRevenue;

    /** Cash collected from COMPLETED transactions in the current calendar year. */
    private BigDecimal currentYearRevenue;

    /**
     * Monthly Recurring Revenue: for every ACTIVE paid subscription, the price
     * actually paid normalised to a per-month figure using the billing cycle
     * that was purchased. Trials contribute zero.
     */
    private BigDecimal mrr;

    /** Annual Recurring Revenue = mrr * 12. */
    private BigDecimal arr;

    /** Business currency these amounts are denominated in (e.g. NPR). */
    private String currency;

    private Long expiringSoon;
    private List<RecentActivityResponse> recentActivities;
    private List<PlanDistributionItem> planDistribution;

    /**
     * Revenue per plan derived from actual COMPLETED transactions. This is the
     * only trustworthy plan revenue figure; it must not be reconstructed from
     * current plan pricing.
     */
    private List<PlanRevenueItem> planRevenue;

    private List<TimeSeriesItem> subscriptionGrowth;
    private List<TimeSeriesItem> revenueGrowth;
    private List<TimeSeriesItem> activeSubscribersTrend;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlanDistributionItem {
        private String planName;
        private Long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlanRevenueItem {
        private Long planId;
        private String planName;
        /** Current ACTIVE/TRIAL subscribers on this plan. */
        private Long subscriberCount;
        /** Number of COMPLETED transactions in the reporting window. */
        private Long transactionCount;
        /** Sum of finalAmount for COMPLETED transactions. */
        private BigDecimal revenue;
        /** Sum of coupon discounts granted. */
        private BigDecimal discount;
        /** Sum of VAT collected. */
        private BigDecimal tax;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimeSeriesItem {
        private String date;
        private Long count;
        private BigDecimal amount;
    }
}
