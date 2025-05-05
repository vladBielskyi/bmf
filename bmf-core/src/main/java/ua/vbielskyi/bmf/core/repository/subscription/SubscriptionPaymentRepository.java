package ua.vbielskyi.bmf.core.repository.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.subscription.SubscriptionPaymentEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, UUID> {

    List<SubscriptionPaymentEntity> findAllBySubscriptionId(UUID subscriptionId);

    List<SubscriptionPaymentEntity> findAllByTenantId(UUID tenantId);
}