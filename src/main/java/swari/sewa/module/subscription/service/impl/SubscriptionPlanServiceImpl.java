package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.CreateSubscriptionPlanRequest;
import swari.sewa.module.subscription.dto.FeatureDto;
import swari.sewa.module.subscription.dto.PricingDto;
import swari.sewa.module.subscription.dto.RestrictionDto;
import swari.sewa.module.subscription.dto.SubscriptionPlanResponse;
import swari.sewa.module.subscription.dto.UpdateSubscriptionPlanRequest;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanFeature;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.enums.PlanCategory;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.PlanVisibility;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.exception.DuplicateSlugException;
import swari.sewa.module.subscription.exception.InvalidSubscriptionStateException;
import swari.sewa.module.subscription.exception.PlanNotAvailableException;
import swari.sewa.module.subscription.exception.SubscriptionPlanNotFoundException;
import swari.sewa.module.subscription.repository.SubscriptionPlanRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionAuditService auditService;

    @Override
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request, Long adminUserId) {
        if (planRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateSlugException("Plan with slug '" + request.getSlug() + "' already exists");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .category(parseCategory(request.getCategory()))
                .icon(request.getIcon())
                .themeColor(request.getThemeColor() != null ? request.getThemeColor() : "#f97316")
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isPopular(request.getIsPopular() != null ? request.getIsPopular() : false)
                .isRecommended(request.getIsRecommended() != null ? request.getIsRecommended() : false)
                .visibility(parseVisibility(request.getVisibility()))
                .status(PlanStatus.DRAFT)
                .pricings(new HashSet<>())
                .restrictions(new HashSet<>())
                .features(new HashSet<>())
                .build();

        if (request.getPricing() != null) {
            SubscriptionPlanPricing pricing = mapPricingDtoToEntity(request.getPricing(), plan);
            plan.getPricings().add(pricing);
        }

        if (request.getRestrictions() != null) {
            SubscriptionPlanRestriction restriction = mapRestrictionDtoToEntity(request.getRestrictions(), plan);
            plan.getRestrictions().add(restriction);
        }

        if (request.getFeatures() != null) {
            for (FeatureDto featureDto : request.getFeatures()) {
                SubscriptionPlanFeature feature = mapFeatureDtoToEntity(featureDto, plan);
                plan.getFeatures().add(feature);
            }
        }

        plan = planRepository.save(plan);
        auditService.recordActivity(SubscriptionAction.PLAN_CREATED, "PLAN", plan.getId(), adminUserId, "Plan '" + plan.getName() + "' created");

        log.info("Plan created with id {} by admin {}", plan.getId(), adminUserId);
        return mapToResponse(plan);
    }

    @Override
    public SubscriptionPlanResponse updatePlan(Long id, UpdateSubscriptionPlanRequest request, Long adminUserId) {
        SubscriptionPlan plan = findPlanById(id);

        plan.setName(request.getName());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getShortDescription() != null) plan.setShortDescription(request.getShortDescription());
        if (request.getCategory() != null) plan.setCategory(parseCategory(request.getCategory()));
        if (request.getIcon() != null) plan.setIcon(request.getIcon());
        if (request.getThemeColor() != null) plan.setThemeColor(request.getThemeColor());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());
        if (request.getIsPopular() != null) plan.setIsPopular(request.getIsPopular());
        if (request.getIsRecommended() != null) plan.setIsRecommended(request.getIsRecommended());
        if (request.getVisibility() != null) plan.setVisibility(parseVisibility(request.getVisibility()));

        // Update pricing (single pricing per plan, stored in a Set)
        if (request.getPricing() != null) {
            plan.getPricings().clear();
            SubscriptionPlanPricing pricing = mapPricingDtoToEntity(request.getPricing(), plan);
            plan.getPricings().add(pricing);
        }

        // Update restrictions (single restriction per plan, stored in a Set)
        if (request.getRestrictions() != null) {
            plan.getRestrictions().clear();
            SubscriptionPlanRestriction restriction = mapRestrictionDtoToEntity(request.getRestrictions(), plan);
            plan.getRestrictions().add(restriction);
        }

        // Update features
        if (request.getFeatures() != null) {
            plan.getFeatures().clear();
            for (FeatureDto featureDto : request.getFeatures()) {
                SubscriptionPlanFeature feature = mapFeatureDtoToEntity(featureDto, plan);
                plan.getFeatures().add(feature);
            }
        }

        plan = planRepository.save(plan);
        auditService.recordActivity(SubscriptionAction.PLAN_UPDATED, "PLAN", plan.getId(), adminUserId, "Plan '" + plan.getName() + "' updated");

        log.info("Plan {} updated by admin {}", plan.getId(), adminUserId);
        return mapToResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Long id) {
        SubscriptionPlan plan = findPlanByIdWithDetails(id);
        return mapToResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionPlanResponse> getPlans(String search, String status, String visibility, String category, Pageable pageable) {
        PlanStatus statusEnum = (status != null && !status.isEmpty()) ? PlanStatus.valueOf(status.toUpperCase()) : null;
        PlanVisibility visibilityEnum = (visibility != null && !visibility.isEmpty()) ? PlanVisibility.valueOf(visibility.toUpperCase()) : null;
        PlanCategory categoryEnum = (category != null && !category.isEmpty()) ? PlanCategory.valueOf(category.toUpperCase()) : null;
        return planRepository.findWithFilters(search, statusEnum, visibilityEnum, categoryEnum, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPublishedPlans() {
        return planRepository.findByStatusOrderBySortOrderAsc(PlanStatus.PUBLISHED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deletePlan(Long id, Long adminUserId) {
        SubscriptionPlan plan = findPlanById(id);

        Long activeSubscriptions = planRepository.countActiveSubscriptionsByPlanId(id);
        if (activeSubscriptions != null && activeSubscriptions > 0) {
            throw new InvalidSubscriptionStateException("Cannot delete plan with " + activeSubscriptions + " active subscriptions");
        }

        if (plan.getStatus() == PlanStatus.PUBLISHED) {
            throw new PlanNotAvailableException("Cannot delete a published plan. Archive it first.");
        }

        planRepository.delete(plan);
        auditService.recordActivity(SubscriptionAction.PLAN_DELETED, "PLAN", id, adminUserId, "Plan '" + plan.getName() + "' deleted");

        log.info("Plan {} deleted by admin {}", id, adminUserId);
    }

    @Override
    public SubscriptionPlanResponse publishPlan(Long id, Long adminUserId) {
        SubscriptionPlan plan = findPlanById(id);

        if (plan.getStatus() == PlanStatus.PUBLISHED) {
            throw new PlanNotAvailableException("Plan is already published");
        }

        if (plan.getStatus() == PlanStatus.DISABLED) {
            throw new PlanNotAvailableException("Cannot publish a disabled plan");
        }

        // Validate mandatory fields for publishing
        if (plan.getName() == null || plan.getName().isBlank()) {
            throw new PlanNotAvailableException("Plan name is required to publish");
        }
        if (plan.getSlug() == null || plan.getSlug().isBlank()) {
            throw new PlanNotAvailableException("Plan slug is required to publish");
        }
        if (plan.getCategory() == null) {
            throw new PlanNotAvailableException("Plan category is required to publish");
        }

        // Validate pricing exists
        if (plan.getPricings() == null || plan.getPricings().isEmpty()) {
            throw new PlanNotAvailableException("Plan must have at least one pricing to publish");
        }

        // Validate slug uniqueness (in case it was changed)
        SubscriptionPlan existing = planRepository.findBySlug(plan.getSlug()).orElse(null);
        if (existing != null && !existing.getId().equals(plan.getId())) {
            throw new DuplicateSlugException("Plan with slug '" + plan.getSlug() + "' already exists");
        }

        plan.setStatus(PlanStatus.PUBLISHED);
        plan = planRepository.save(plan);
        auditService.recordActivity(SubscriptionAction.PLAN_PUBLISHED, "PLAN", plan.getId(), adminUserId, "Plan '" + plan.getName() + "' published");

        log.info("Plan {} published by admin {}", plan.getId(), adminUserId);
        return mapToResponse(plan);
    }

    @Override
    public SubscriptionPlanResponse archivePlan(Long id, Long adminUserId) {
        SubscriptionPlan plan = findPlanById(id);

        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new PlanNotAvailableException("Plan is already archived");
        }

        if (plan.getStatus() == PlanStatus.DRAFT) {
            throw new PlanNotAvailableException("Cannot archive a draft plan. Publish or delete it instead.");
        }

        plan.setStatus(PlanStatus.ARCHIVED);
        plan = planRepository.save(plan);
        auditService.recordActivity(SubscriptionAction.PLAN_ARCHIVED, "PLAN", plan.getId(), adminUserId, "Plan '" + plan.getName() + "' archived");

        log.info("Plan {} archived by admin {}", plan.getId(), adminUserId);
        return mapToResponse(plan);
    }

    @Override
    public SubscriptionPlanResponse duplicatePlan(Long id, Long adminUserId) {
        SubscriptionPlan original = findPlanByIdWithDetails(id);

        String newSlug = original.getSlug() + "-copy-" + System.currentTimeMillis();

        SubscriptionPlan copy = SubscriptionPlan.builder()
                .name(original.getName() + " (Copy)")
                .slug(newSlug)
                .description(original.getDescription())
                .shortDescription(original.getShortDescription())
                .category(original.getCategory())
                .icon(original.getIcon())
                .themeColor(original.getThemeColor())
                .sortOrder(original.getSortOrder())
                .isPopular(false)
                .isRecommended(false)
                .visibility(original.getVisibility())
                .status(PlanStatus.DRAFT)
                .pricings(new HashSet<>())
                .restrictions(new HashSet<>())
                .features(new HashSet<>())
                .build();

        // Copy pricing
        if (original.getPricings() != null) {
            for (SubscriptionPlanPricing pricing : original.getPricings()) {
                SubscriptionPlanPricing pricingCopy = SubscriptionPlanPricing.builder()
                        .monthly(pricing.getMonthly())
                        .quarterly(pricing.getQuarterly())
                        .halfYearly(pricing.getHalfYearly())
                        .yearly(pricing.getYearly())
                        .currency(pricing.getCurrency())
                        .gstIncluded(pricing.getGstIncluded())
                        .discountPercentage(pricing.getDiscountPercentage())
                        .strikePrice(pricing.getStrikePrice())
                        .plan(copy)
                        .build();
                copy.getPricings().add(pricingCopy);
            }
        }

        // Copy restrictions
        if (original.getRestrictions() != null) {
            for (SubscriptionPlanRestriction restriction : original.getRestrictions()) {
                SubscriptionPlanRestriction restrictionCopy = SubscriptionPlanRestriction.builder()
                        .maxVehicles(restriction.getMaxVehicles())
                        .maxEmployees(restriction.getMaxEmployees())
                        .maxStorage(restriction.getMaxStorage())
                        .maxBranches(restriction.getMaxBranches())
                        .apiCalls(restriction.getApiCalls())
                        .supportLevel(restriction.getSupportLevel())
                        .dailyUploadLimit(restriction.getDailyUploadLimit())
                        .backupFrequency(restriction.getBackupFrequency())
                        .plan(copy)
                        .build();
                copy.getRestrictions().add(restrictionCopy);
            }
        }

        // Copy features
        if (original.getFeatures() != null) {
            for (SubscriptionPlanFeature feature : original.getFeatures()) {
                SubscriptionPlanFeature featureCopy = SubscriptionPlanFeature.builder()
                        .name(feature.getName())
                        .icon(feature.getIcon())
                        .description(feature.getDescription())
                        .included(feature.getIncluded())
                        .limit(feature.getLimit())
                        .plan(copy)
                        .build();
                copy.getFeatures().add(featureCopy);
            }
        }

        copy = planRepository.save(copy);
        auditService.recordActivity(SubscriptionAction.PLAN_DUPLICATED, "PLAN", copy.getId(), adminUserId, "Plan '" + original.getName() + "' duplicated as '" + copy.getName() + "'");

        log.info("Plan {} duplicated as {} by admin {}", original.getId(), copy.getId(), adminUserId);
        return mapToResponse(copy);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanEntity(Long id) {
        return findPlanById(id);
    }

    // ==============================
    // Private helper methods
    // ==============================

    private SubscriptionPlan findPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new SubscriptionPlanNotFoundException("Subscription plan not found with id: " + id));
    }

    private SubscriptionPlan findPlanByIdWithDetails(Long id) {
        return planRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new SubscriptionPlanNotFoundException("Subscription plan not found with id: " + id));
    }

    private PlanCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return PlanCategory.CUSTOM;
        }
        try {
            return PlanCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlanCategory.CUSTOM;
        }
    }

    private PlanVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return PlanVisibility.PUBLIC;
        }
        try {
            return PlanVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlanVisibility.PUBLIC;
        }
    }

    private SubscriptionPlanPricing mapPricingDtoToEntity(PricingDto dto, SubscriptionPlan plan) {
        return SubscriptionPlanPricing.builder()
                .monthly(dto.getMonthly())
                .quarterly(dto.getQuarterly())
                .halfYearly(dto.getHalfYearly())
                .yearly(dto.getYearly())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .gstIncluded(dto.getGstIncluded() != null ? dto.getGstIncluded() : true)
                .discountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0)
                .strikePrice(dto.getStrikePrice())
                .plan(plan)
                .build();
    }

    private SubscriptionPlanRestriction mapRestrictionDtoToEntity(RestrictionDto dto, SubscriptionPlan plan) {
        return SubscriptionPlanRestriction.builder()
                .maxVehicles(dto.getMaxVehicles())
                .maxEmployees(dto.getMaxEmployees())
                .maxStorage(dto.getMaxStorage())
                .maxBranches(dto.getMaxBranches())
                .apiCalls(dto.getApiCalls())
                .supportLevel(dto.getSupportLevel())
                .dailyUploadLimit(dto.getDailyUploadLimit())
                .backupFrequency(dto.getBackupFrequency())
                .plan(plan)
                .build();
    }

    private SubscriptionPlanFeature mapFeatureDtoToEntity(FeatureDto dto, SubscriptionPlan plan) {
        return SubscriptionPlanFeature.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .description(dto.getDescription())
                .included(dto.getIncluded() != null ? dto.getIncluded() : false)
                .limit(dto.getLimit())
                .plan(plan)
                .build();
    }

    private PricingDto mapPricingToDto(SubscriptionPlanPricing pricing) {
        if (pricing == null) {
            return null;
        }
        return PricingDto.builder()
                .monthly(pricing.getMonthly())
                .quarterly(pricing.getQuarterly())
                .halfYearly(pricing.getHalfYearly())
                .yearly(pricing.getYearly())
                .currency(pricing.getCurrency())
                .gstIncluded(pricing.getGstIncluded())
                .discountPercentage(pricing.getDiscountPercentage())
                .strikePrice(pricing.getStrikePrice())
                .build();
    }

    private RestrictionDto mapRestrictionToDto(SubscriptionPlanRestriction restriction) {
        if (restriction == null) {
            return null;
        }
        return RestrictionDto.builder()
                .maxVehicles(restriction.getMaxVehicles())
                .maxEmployees(restriction.getMaxEmployees())
                .maxStorage(restriction.getMaxStorage())
                .maxBranches(restriction.getMaxBranches())
                .apiCalls(restriction.getApiCalls())
                .supportLevel(restriction.getSupportLevel())
                .dailyUploadLimit(restriction.getDailyUploadLimit())
                .backupFrequency(restriction.getBackupFrequency())
                .build();
    }

    private FeatureDto mapFeatureToDto(SubscriptionPlanFeature feature) {
        return FeatureDto.builder()
                .id(feature.getId())
                .name(feature.getName())
                .icon(feature.getIcon())
                .description(feature.getDescription())
                .included(feature.getIncluded())
                .limit(feature.getLimit())
                .build();
    }

    private SubscriptionPlanResponse mapToResponse(SubscriptionPlan plan) {
        // Extract single pricing from the Set (first element)
        PricingDto pricingDto = null;
        if (plan.getPricings() != null && !plan.getPricings().isEmpty()) {
            pricingDto = mapPricingToDto(plan.getPricings().iterator().next());
        }

        // Extract single restriction from the Set (first element)
        RestrictionDto restrictionDto = null;
        if (plan.getRestrictions() != null && !plan.getRestrictions().isEmpty()) {
            restrictionDto = mapRestrictionToDto(plan.getRestrictions().iterator().next());
        }

        // Map features list
        List<FeatureDto> featureDtos = new ArrayList<>();
        if (plan.getFeatures() != null) {
            for (SubscriptionPlanFeature feature : plan.getFeatures()) {
                featureDtos.add(mapFeatureToDto(feature));
            }
        }

        Long subscriberCount = planRepository.countActiveSubscriptionsByPlanId(plan.getId());

        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .slug(plan.getSlug())
                .description(plan.getDescription())
                .shortDescription(plan.getShortDescription())
                .category(plan.getCategory() != null ? plan.getCategory().name() : null)
                .icon(plan.getIcon())
                .themeColor(plan.getThemeColor())
                .sortOrder(plan.getSortOrder())
                .isPopular(plan.getIsPopular())
                .isRecommended(plan.getIsRecommended())
                .visibility(plan.getVisibility() != null ? plan.getVisibility().name() : null)
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .createdDate(plan.getCreatedAt())
                .updatedDate(plan.getUpdatedAt())
                .pricing(pricingDto)
                .restrictions(restrictionDto)
                .features(featureDtos)
                .subscriberCount(subscriberCount)
                .build();
    }
}
