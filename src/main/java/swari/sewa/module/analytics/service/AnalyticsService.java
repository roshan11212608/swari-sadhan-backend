package swari.sewa.module.analytics.service;

import swari.sewa.module.analytics.dto.AnalyticsDashboardResponse;

public interface AnalyticsService {
    AnalyticsDashboardResponse getDashboard(Long shopId, String filter);
}
