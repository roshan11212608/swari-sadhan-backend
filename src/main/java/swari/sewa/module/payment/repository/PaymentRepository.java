package swari.sewa.module.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionUuid(String transactionUuid);

    boolean existsByTransactionUuid(String transactionUuid);

    List<Payment> findByShopOwnerIdAndStatusOrderByPaidAtDesc(Long shopOwnerId, PaymentStatus status);
}
