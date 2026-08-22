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
import java.util.Optional;

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

        Optional<SubscriptionInvoiceSequence> seqOpt = sequenceRepository.findAll().stream()
                .filter(s -> s.getYear().equals(year))
                .findFirst();

        int nextVal;
        if (seqOpt.isEmpty()) {
            SubscriptionInvoiceSequence seq = SubscriptionInvoiceSequence.builder()
                    .year(year)
                    .nextVal(2)
                    .build();
            sequenceRepository.save(seq);
            nextVal = 1;
        } else {
            SubscriptionInvoiceSequence seq = seqOpt.get();
            nextVal = seq.getNextVal();
            seq.setNextVal(nextVal + 1);
            sequenceRepository.save(seq);
        }

        String invoiceNumber = String.format("%s-%d-%06d", prefix, year, nextVal);
        log.info("Generated invoice number: {}", invoiceNumber);
        return invoiceNumber;
    }
}
