package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.util.ReportingClock;
import swari.sewa.module.subscription.dto.RecentActivityResponse;
import swari.sewa.module.subscription.dto.SubscriptionDashboardResponse;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.repository.SubscriptionPlanRepository;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionDashboardService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionDashboardServiceImpl implements SubscriptionDashboardService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTransactionRepository transactionRepository;
    private final SubscriptionAuditService auditService;
    private final SubscriptionSettingsService settingsService;
    private final ReportingClock clock;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDashboardResponse getDashboard(String timeRange) {
        log.info("Fetching subscription dashboard for timeRange: {}", timeRange);

        // All reporting boundaries come from the business timezone, never the
        // JVM default. See ReportingClock for the exact range semantics.
        LocalDateTime now = clock.now();
        LocalDateTime fromDate = clock.resolveRangeStart(timeRange);

        long totalPlans = planRepository.count();
        long activePlans = planRepository.countByStatus(PlanStatus.PUBLISHED);
        long draftPlans = planRepository.countByStatus(PlanStatus.DRAFT);

        long totalSubscribers = subscriptionRepository.countActiveSubscriptions();
        long activeTrials = subscriptionRepository.countActiveTrials();

        BigDecimal currentMonthRevenue = zeroIfNull(
                transactionRepository.sumCompletedAmountBetween(clock.startOfCurrentMonth(), now));
        BigDecimal currentYearRevenue = zeroIfNull(
                transactionRepository.sumCompletedAmountBetween(clock.startOfCurrentYear(), now));

        BigDecimal mrr = calculateMrr();
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);

        SubscriptionSettings settings = settingsService.getSettingsEntity();
        int renewalReminder = settings.getRenewalReminder() != null ? settings.getRenewalReminder() : 3;
        long expiringSoon = subscriptionRepository.findExpiringSoon(now.plusDays(renewalReminder)).size();

        List<RecentActivityResponse> recentActivities = auditService
                .getRecentActivities(PageRequest.of(0, 15))
                .getContent();

        List<SubscriptionDashboardResponse.PlanDistributionItem> planDistribution = buildPlanDistribution();
        List<SubscriptionDashboardResponse.PlanRevenueItem> planRevenue = buildPlanRevenue(fromDate, now);
        List<SubscriptionDashboardResponse.TimeSeriesItem> subscriptionGrowth = buildSubscriptionGrowth(fromDate);
        List<SubscriptionDashboardResponse.TimeSeriesItem> revenueGrowth = buildRevenueGrowth(fromDate);
        List<SubscriptionDashboardResponse.TimeSeriesItem> activeSubscribersTrend = buildActiveSubscribersTrend(now);

        return SubscriptionDashboardResponse.builder()
                .totalPlans(totalPlans)
                .activePlans(activePlans)
                .draftPlans(draftPlans)
                .totalSubscribers(totalSubscribers)
                .activeTrials(activeTrials)
                // Deprecated aliases kept so existing clients keep working.
                .monthlyRevenue(currentMonthRevenue)
                .yearlyRevenue(currentYearRevenue)
                .currentMonthRevenue(currentMonthRevenue)
                .currentYearRevenue(currentYearRevenue)
                .mrr(mrr)
                .arr(arr)
                .currency(settings.getCurrency() != null ? settings.getCurrency() : "NPR")
                .expiringSoon(expiringSoon)
                .recentActivities(recentActivities)
                .planDistribution(planDistribution)
                .planRevenue(planRevenue)
                .subscriptionGrowth(subscriptionGrowth)
                .revenueGrowth(revenueGrowth)
                .activeSubscribersTrend(activeSubscribersTrend)
                .build();
    }

    /**
     * Monthly Recurring Revenue.
     *
     * <p>Each ACTIVE paid subscription contributes the price it actually paid,
     * normalised to a monthly figure by the billing cycle that was purchased.
     * A yearly subscription of 12,000 contributes 1,000/month.
     *
     * <p>TRIAL subscriptions are excluded (no recurring revenue) and rows with a
     * null price contribute nothing.
     */
    private BigDecimal calculateMrr() {
        BigDecimal mrr = BigDecimal.ZERO;
        for (Object[] row : subscriptionRepository.sumActivePricePaidByBillingCycle()) {
            String cycle = (String) row[0];
            BigDecimal total = zeroIfNull((BigDecimal) row[1]);
            int months = monthsInCycle(cycle);
            if (months <= 0) {
                continue;
            }
            mrr = mrr.add(total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP));
        }
        return mrr.setScale(2, RoundingMode.HALF_UP);
    }

    /** Number of months covered by a billing cycle. Unknown cycles are skipped. */
    private int monthsInCycle(String billingCycle) {
        if (billingCycle == null) {
            return 1;
        }
        return switch (billingCycle.trim().toLowerCase()) {
            case "monthly" -> 1;
            case "quarterly" -> 3;
            case "halfyearly", "half_yearly", "half-yearly", "semiannual" -> 6;
            case "yearly", "annual" -> 12;
            default -> {
                log.warn("Unknown billing cycle '{}' encountered while computing MRR — skipping", billingCycle);
                yield 0;
            }
        };
    }

    /**
     * Plan revenue from actual COMPLETED transactions, joined with current
     * subscriber counts. Revenue is never inferred from present-day plan
     * pricing, because historical transactions carry their own price, billing
     * cycle and discounts.
     */
    private List<SubscriptionDashboardResponse.PlanRevenueItem> buildPlanRevenue(LocalDateTime fromDate,
                                                                                LocalDateTime toDate) {
        Map<Long, Long> subscribersByPlan = subscriptionRepository.countSubscribersByPlan().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<Object[]> rows = transactionRepository.getRevenueByPlan(fromDate, toDate);
        List<SubscriptionDashboardResponse.PlanRevenueItem> items = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();

        for (Object[] row : rows) {
            Long planId = (Long) row[0];
            seen.add(planId);
            items.add(SubscriptionDashboardResponse.PlanRevenueItem.builder()
                    .planId(planId)
                    .planName((String) row[1])
                    .transactionCount((Long) row[2])
                    .revenue(zeroIfNull((BigDecimal) row[3]))
                    .discount(zeroIfNull((BigDecimal) row[4]))
                    .tax(zeroIfNull((BigDecimal) row[5]))
                    .subscriberCount(subscribersByPlan.getOrDefault(planId, 0L))
                    .build());
        }

        // Include plans that currently have subscribers but produced no revenue
        // in this window, so the report shows them at zero rather than hiding them.
        List<Long> missing = subscribersByPlan.keySet().stream()
                .filter(id -> !seen.contains(id))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            Map<Long, String> names = planRepository.findAllById(missing).stream()
                    .collect(Collectors.toMap(SubscriptionPlan::getId, SubscriptionPlan::getName));
            for (Long planId : missing) {
                items.add(SubscriptionDashboardResponse.PlanRevenueItem.builder()
                        .planId(planId)
                        .planName(names.getOrDefault(planId, "Unknown"))
                        .transactionCount(0L)
                        .revenue(BigDecimal.ZERO)
                        .discount(BigDecimal.ZERO)
                        .tax(BigDecimal.ZERO)
                        .subscriberCount(subscribersByPlan.getOrDefault(planId, 0L))
                        .build());
            }
        }
        return items;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<SubscriptionDashboardResponse.PlanDistributionItem> buildPlanDistribution() {
        List<Object[]> results = subscriptionRepository.countSubscribersByPlan();
        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> planIds = results.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        Map<Long, String> planNames = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, SubscriptionPlan::getName));

        return results.stream()
                .map(row -> {
                    Long planId = (Long) row[0];
                    Long count = (Long) row[1];
                    return SubscriptionDashboardResponse.PlanDistributionItem.builder()
                            .planName(planNames.getOrDefault(planId, "Unknown"))
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SubscriptionDashboardResponse.TimeSeriesItem> buildSubscriptionGrowth(LocalDateTime fromDate) {
        List<Object[]> results = transactionRepository.getSubscriptionGrowthByMonth(fromDate);
        return results.stream()
                .map(row -> {
                    String month = (String) row[0];
                    Long count = (Long) row[1];
                    return SubscriptionDashboardResponse.TimeSeriesItem.builder()
                            .date(month)
                            .count(count)
                            .amount(BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SubscriptionDashboardResponse.TimeSeriesItem> buildRevenueGrowth(LocalDateTime fromDate) {
        List<Object[]> results = transactionRepository.getRevenueGrowthByMonth(fromDate);
        return results.stream()
                .map(row -> {
                    String month = (String) row[0];
                    Long count = (Long) row[1];
                    BigDecimal amount = (BigDecimal) row[2];
                    return SubscriptionDashboardResponse.TimeSeriesItem.builder()
                            .date(month)
                            .count(count)
                            .amount(amount != null ? amount : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SubscriptionDashboardResponse.TimeSeriesItem> buildActiveSubscribersTrend(LocalDateTime now) {
        List<Object[]> results = subscriptionRepository.getActiveSubscribersTrendByMonth(now);
        return results.stream()
                .map(row -> {
                    String month = (String) row[0];
                    Long count = (Long) row[1];
                    return SubscriptionDashboardResponse.TimeSeriesItem.builder()
                            .date(month)
                            .count(count)
                            .amount(BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
