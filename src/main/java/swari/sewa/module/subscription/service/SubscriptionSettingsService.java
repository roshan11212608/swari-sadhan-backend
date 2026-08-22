package swari.sewa.module.subscription.service;

import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.entity.SubscriptionSettings;

public interface SubscriptionSettingsService {
    SubscriptionSettingsResponse getSettings();
    SubscriptionSettingsResponse updateSettings(UpdateSubscriptionSettingsRequest request, Long adminUserId);
    SubscriptionSettings getSettingsEntity();
}
