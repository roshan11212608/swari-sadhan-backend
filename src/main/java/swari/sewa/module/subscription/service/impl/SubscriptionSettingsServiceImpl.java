package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.SubscriptionSettingsResponse;
import swari.sewa.module.subscription.dto.UpdateSubscriptionSettingsRequest;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.repository.SubscriptionSettingsRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionSettingsServiceImpl implements SubscriptionSettingsService {

    private final SubscriptionSettingsRepository settingsRepository;
    private final SubscriptionAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSettingsResponse getSettings() {
        SubscriptionSettings settings = getOrCreateSettings();
        return mapToResponse(settings);
    }

    @Override
    public SubscriptionSettingsResponse updateSettings(UpdateSubscriptionSettingsRequest request, Long adminUserId) {
        SubscriptionSettings settings = getOrCreateSettings();

        if (request.getDefaultTrialDays() != null) settings.setDefaultTrialDays(request.getDefaultTrialDays());
        if (request.getTaxPercentage() != null) settings.setTaxPercentage(request.getTaxPercentage());
        if (request.getCurrency() != null) settings.setCurrency(request.getCurrency());
        if (request.getInvoicePrefix() != null) settings.setInvoicePrefix(request.getInvoicePrefix());
        if (request.getPaymentReminderDays() != null) settings.setPaymentReminderDays(request.getPaymentReminderDays());
        if (request.getRenewalReminder() != null) settings.setRenewalReminder(request.getRenewalReminder());
        if (request.getGracePeriod() != null) settings.setGracePeriod(request.getGracePeriod());
        if (request.getCancellationPolicy() != null) settings.setCancellationPolicy(request.getCancellationPolicy());
        if (request.getRefundPolicy() != null) settings.setRefundPolicy(request.getRefundPolicy());
        if (request.getEnableAutoRenewal() != null) settings.setEnableAutoRenewal(request.getEnableAutoRenewal());
        if (request.getEnableFreeTrial() != null) settings.setEnableFreeTrial(request.getEnableFreeTrial());
        if (request.getEnableCoupons() != null) settings.setEnableCoupons(request.getEnableCoupons());
        if (request.getEnableLifetimePlans() != null) settings.setEnableLifetimePlans(request.getEnableLifetimePlans());
        if (request.getEnableVat() != null) settings.setEnableVat(request.getEnableVat());

        settings = settingsRepository.save(settings);
        auditService.recordActivity(SubscriptionAction.SETTINGS_UPDATED, "SETTINGS", settings.getId(), adminUserId, "Subscription settings updated");

        log.info("Subscription settings updated by admin {}", adminUserId);
        return mapToResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSettings getSettingsEntity() {
        return getOrCreateSettings();
    }

    private SubscriptionSettings getOrCreateSettings() {
        return settingsRepository.findById(1L).orElseGet(() -> {
            SubscriptionSettings defaults = SubscriptionSettings.builder()
                    .id(1L)
                    .defaultTrialDays(14)
                    .taxPercentage(18)
                    .currency("INR")
                    .invoicePrefix("INV")
                    .paymentReminderDays(7)
                    .renewalReminder(3)
                    .gracePeriod(5)
                    .cancellationPolicy("Users can cancel anytime. Refunds processed within 7 days.")
                    .refundPolicy("Full refund within 7 days, prorated refund after that.")
                    .enableAutoRenewal(true)
                    .enableFreeTrial(true)
                    .enableCoupons(true)
                    .enableLifetimePlans(true)
                    .enableVat(true)
                    .build();
            log.info("Creating default subscription settings");
            return settingsRepository.save(defaults);
        });
    }

    private SubscriptionSettingsResponse mapToResponse(SubscriptionSettings s) {
        return SubscriptionSettingsResponse.builder()
                .id(s.getId())
                .defaultTrialDays(s.getDefaultTrialDays())
                .taxPercentage(s.getTaxPercentage())
                .currency(s.getCurrency())
                .invoicePrefix(s.getInvoicePrefix())
                .paymentReminderDays(s.getPaymentReminderDays())
                .renewalReminder(s.getRenewalReminder())
                .gracePeriod(s.getGracePeriod())
                .cancellationPolicy(s.getCancellationPolicy())
                .refundPolicy(s.getRefundPolicy())
                .enableAutoRenewal(s.getEnableAutoRenewal())
                .enableFreeTrial(s.getEnableFreeTrial())
                .enableCoupons(s.getEnableCoupons())
                .enableLifetimePlans(s.getEnableLifetimePlans())
                .enableVat(s.getEnableVat())
                .createdDate(s.getCreatedAt())
                .updatedDate(s.getUpdatedAt())
                .build();
    }
}
