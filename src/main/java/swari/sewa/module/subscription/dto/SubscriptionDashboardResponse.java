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
    private BigDecimal monthlyRevenue;
    private BigDecimal yearlyRevenue;
    private Long expiringSoon;
    private List<RecentActivityResponse> recentActivities;
    private List<PlanDistributionItem> planDistribution;
    private List<TimeSeriesItem> subscriptionGrowth;
    private List<TimeSeriesItem> revenueGrowth;
    private List<TimeSeriesItem> activeSubscribersTrend;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlanDistributionItem {
        private String planName;
        private Long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimeSeriesItem {
        private String date;
        private Long count;
        private BigDecimal amount;
    }
}
