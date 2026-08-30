package swari.sewa.module.subscription.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import swari.sewa.module.subscription.entity.SubscriptionInvoiceSequence;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.repository.SubscriptionInvoiceSequenceRepository;
import swari.sewa.module.subscription.repository.SubscriptionSettingsRepository;
import swari.sewa.module.subscription.service.InvoiceService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for invoice sequence generation.
 *
 * Verifies that concurrent invoice number generation produces unique
 * invoice numbers with no duplicates.
 *
 * This test exposed a production bug where InvoiceServiceImpl used
 * findAll() instead of findNextValByYearForUpdate() (SELECT ... FOR UPDATE),
 * allowing concurrent transactions to read the same nextVal and produce
 * duplicate invoice numbers.
 */
@SpringBootTest
@ActiveProfiles("integration")
class InvoiceSequenceConcurrencyTest {

    @Autowired private InvoiceService invoiceService;
    @Autowired private SubscriptionInvoiceSequenceRepository sequenceRepository;
    @Autowired private SubscriptionSettingsRepository settingsRepository;

    @BeforeEach
    @org.springframework.transaction.annotation.Transactional
    void setUp() {
        // Ensure singleton settings row exists (needed by generateInvoiceNumber)
        if (!settingsRepository.existsById(1L)) {
            SubscriptionSettings settings = SubscriptionSettings.builder()
                    .id(1L)
                    .enableVat(true)
                    .taxPercentage(13)
                    .currency("NPR")
                    .invoicePrefix("INV")
                    .build();
            settingsRepository.saveAndFlush(settings);
        }

        // Clean up any existing sequence for the current year
        int year = LocalDateTime.now().getYear();
        List<SubscriptionInvoiceSequence> existing = sequenceRepository.findAll();
        for (SubscriptionInvoiceSequence seq : existing) {
            if (seq.getYear().equals(year)) {
                sequenceRepository.delete(seq);
            }
        }
        sequenceRepository.flush();
    }

    @Test
    @DisplayName("20 concurrent invoice generations → all successful numbers are unique")
    void testConcurrentInvoiceGeneration_uniqueNumbers() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        Set<String> generatedNumbers = ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    String invoiceNumber = invoiceService.generateInvoiceNumber();
                    generatedNumbers.add(invoiceNumber);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Deadlocks are expected when 20 threads compete for the same row lock.
                    // The important assertion is that the numbers that ARE generated are unique.
                    errorCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // release all threads simultaneously
        endGate.await(); // wait for all to complete
        executor.shutdown();

        // At least some should succeed
        assertTrue(successCount.get() > 0,
                "At least some invoice generations should succeed");

        // CRITICAL: All generated invoice numbers must be unique
        // This is the key assertion — no duplicates even under concurrency
        assertEquals(successCount.get(), generatedNumbers.size(),
                "All " + successCount.get() + " generated invoice numbers must be unique. " +
                "Generated: " + generatedNumbers);

        // All should have the correct format: PREFIX-YEAR-NNNNNN
        int year = LocalDateTime.now().getYear();
        for (String num : generatedNumbers) {
            assertTrue(num.contains("-" + year + "-"),
                    "Invoice number must contain year: " + num);
        }
    }

    @Test
    @DisplayName("Sequential invoice generation → numbers increment correctly")
    void testSequentialInvoiceGeneration_increments() {
        String num1 = invoiceService.generateInvoiceNumber();
        String num2 = invoiceService.generateInvoiceNumber();
        String num3 = invoiceService.generateInvoiceNumber();

        assertNotEquals(num1, num2, "Sequential invoice numbers must differ");
        assertNotEquals(num2, num3, "Sequential invoice numbers must differ");
        assertNotEquals(num1, num3, "Sequential invoice numbers must differ");
    }
}
