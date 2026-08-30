package swari.sewa.module.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionInvoiceSequence;

@Repository
public interface SubscriptionInvoiceSequenceRepository extends JpaRepository<SubscriptionInvoiceSequence, Long> {

    @Query(value = "SELECT next_val FROM subscription_invoice_sequence WHERE year = :year FOR UPDATE", nativeQuery = true)
    Integer findNextValByYearForUpdate(@Param("year") Integer year);

    @Modifying
    @Query(value = "UPDATE subscription_invoice_sequence SET next_val = :nextVal WHERE year = :year", nativeQuery = true)
    void incrementNextVal(@Param("year") Integer year, @Param("nextVal") Integer nextVal);
}
