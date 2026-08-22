package swari.sewa.module.subscription.service;

import swari.sewa.module.subscription.dto.*;

public interface SubscriptionDashboardService {
    SubscriptionDashboardResponse getDashboard(String timeRange);
}
