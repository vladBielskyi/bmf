package ua.vbielskyi.bmf.core.repository.notification;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.notification.NotificationHistoryEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationHistoryRepository extends MultiTenantJpaRepository<NotificationHistoryEntity, UUID> {

    List<NotificationHistoryEntity> findAllByTenantIdAndTelegramId(UUID tenantId, Long telegramId);

    List<NotificationHistoryEntity> findAllByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    List<NotificationHistoryEntity> findAllByTenantIdAndCreatedAtBetween(
            UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    List<NotificationHistoryEntity> findAllByTenantIdAndTypeAndSentTrue(UUID tenantId, String type);

    List<NotificationHistoryEntity> findAllByTenantIdAndSentFalse(UUID tenantId);
}