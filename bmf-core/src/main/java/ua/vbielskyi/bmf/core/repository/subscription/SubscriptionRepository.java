package ua.vbielskyi.bmf.core.repository.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.common.model.subscription.SubscriptionStatus;
import ua.vbielskyi.bmf.core.entity.subscription.SubscriptionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByTenantIdAndStatusNot(UUID tenantId, SubscriptionStatus status);

    List<SubscriptionEntity> findByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime start, LocalDateTime end);

    List<SubscriptionEntity> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime date);
}