package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDashboardResponse getDashboard(String timeRange) {
        log.info("Fetching subscription dashboard for timeRange: {}", timeRange);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDate = calculateFromDate(timeRange, now);

        long totalPlans = planRepository.count();
        long activePlans = planRepository.countByStatus(PlanStatus.PUBLISHED);
        long draftPlans = planRepository.countByStatus(PlanStatus.DRAFT);

        long totalSubscribers = subscriptionRepository.countActiveSubscriptions();
        long activeTrials = subscriptionRepository.countActiveTrials();

        LocalDateTime firstDayOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime firstDayOfYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal monthlyRevenue = transactionRepository.sumCompletedAmountBetween(firstDayOfMonth, now);
        BigDecimal yearlyRevenue = transactionRepository.sumCompletedAmountBetween(firstDayOfYear, now);

        SubscriptionSettings settings = settingsService.getSettingsEntity();
        int renewalReminder = settings.getRenewalReminder() != null ? settings.getRenewalReminder() : 3;
        long expiringSoon = subscriptionRepository.findExpiringSoon(now.plusDays(renewalReminder)).size();

        List<RecentActivityResponse> recentActivities = auditService
                .getRecentActivities(PageRequest.of(0, 10))
                .getContent();

        List<SubscriptionDashboardResponse.PlanDistributionItem> planDistribution = buildPlanDistribution();
        List<SubscriptionDashboardResponse.TimeSeriesItem> subscriptionGrowth = buildSubscriptionGrowth(fromDate);
        List<SubscriptionDashboardResponse.TimeSeriesItem> revenueGrowth = buildRevenueGrowth(fromDate);
        List<SubscriptionDashboardResponse.TimeSeriesItem> activeSubscribersTrend = new ArrayList<>();

        return SubscriptionDashboardResponse.builder()
                .totalPlans(totalPlans)
                .activePlans(activePlans)
                .draftPlans(draftPlans)
                .totalSubscribers(totalSubscribers)
                .activeTrials(activeTrials)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .yearlyRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO)
                .expiringSoon(expiringSoon)
                .recentActivities(recentActivities)
                .planDistribution(planDistribution)
                .subscriptionGrowth(subscriptionGrowth)
                .revenueGrowth(revenueGrowth)
                .activeSubscribersTrend(activeSubscribersTrend)
                .build();
    }

    private LocalDateTime calculateFromDate(String timeRange, LocalDateTime now) {
        if (timeRange == null) {
            return now.minusDays(30);
        }
        return switch (timeRange) {
            case "7d" -> now.minusDays(7);
            case "30d" -> now.minusDays(30);
            case "90d" -> now.minusDays(90);
            case "1y" -> now.minusYears(1);
            default -> now.minusDays(30);
        };
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
}
