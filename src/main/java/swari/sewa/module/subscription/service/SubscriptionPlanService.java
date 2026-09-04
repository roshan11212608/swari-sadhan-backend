package swari.sewa.module.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.entity.SubscriptionPlan;

import java.util.Collection;
import java.util.List;

public interface SubscriptionPlanService {
    SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request, Long adminUserId);
    SubscriptionPlanResponse updatePlan(Long id, UpdateSubscriptionPlanRequest request, Long adminUserId);
    SubscriptionPlanResponse getPlanById(Long id);
    Page<SubscriptionPlanResponse> getPlans(String search, String status, String visibility, String category, Pageable pageable);
    List<SubscriptionPlanResponse> getPublishedPlans();
    void deletePlan(Long id, Long adminUserId);
    SubscriptionPlanResponse publishPlan(Long id, Long adminUserId);
    SubscriptionPlanResponse archivePlan(Long id, Long adminUserId);
    SubscriptionPlanResponse duplicatePlan(Long id, Long adminUserId);
    SubscriptionPlan getPlanEntity(Long id);
    List<SubscriptionPlan> findAllById(Collection<Long> ids);
}
