package swari.sewa.module.subscription.service;

import swari.sewa.module.subscription.dto.*;

public interface SubscriptionTrialService {
    TrialResponse getTrial();
    TrialResponse updateTrial(UpdateTrialRequest request, Long adminUserId);
}
