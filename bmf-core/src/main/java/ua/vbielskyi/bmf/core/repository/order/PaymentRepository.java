package ua.vbielskyi.bmf.core.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.common.model.order.PaymentStatus;
import ua.vbielskyi.bmf.core.entity.order.PaymentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(UUID orderId);

    List<PaymentEntity> findByStatus(PaymentStatus status);

    Optional<PaymentEntity> findByTransactionId(String transactionId);
}