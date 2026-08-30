package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.entity.SubscriptionInvoiceSequence;
import swari.sewa.module.subscription.repository.SubscriptionInvoiceSequenceRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final SubscriptionInvoiceSequenceRepository sequenceRepository;
    private final SubscriptionSettingsService settingsService;

    @Override
    public String generateInvoiceNumber() {
        int year = LocalDateTime.now().getYear();
        String prefix = settingsService.getSettingsEntity().getInvoicePrefix();

        // Use pessimistic locking (SELECT ... FOR UPDATE) to prevent concurrent
        // invoice generations from reading the same nextVal and producing duplicates.
        Integer currentNextVal = sequenceRepository.findNextValByYearForUpdate(year);

        int nextVal;
        if (currentNextVal == null) {
            // No sequence row for this year — create one starting at 2 (we return 1)
            SubscriptionInvoiceSequence seq = SubscriptionInvoiceSequence.builder()
                    .year(year)
                    .nextVal(2)
                    .build();
            sequenceRepository.saveAndFlush(seq);
            nextVal = 1;
        } else {
            // Update the existing row with the next value
            // The pessimistic lock ensures no other transaction can read this row
            // until our transaction commits
            nextVal = currentNextVal;
            sequenceRepository.incrementNextVal(year, currentNextVal + 1);
            sequenceRepository.flush();
        }

        String invoiceNumber = String.format("%s-%d-%06d", prefix, year, nextVal);
        log.info("Generated invoice number: {}", invoiceNumber);
        return invoiceNumber;
    }
}
