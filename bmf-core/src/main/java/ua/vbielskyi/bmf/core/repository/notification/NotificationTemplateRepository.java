package ua.vbielskyi.bmf.core.repository.notification;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.notification.NotificationTemplateEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends MultiTenantJpaRepository<NotificationTemplateEntity, UUID> {

    List<NotificationTemplateEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<NotificationTemplateEntity> findByTenantIdAndTypeAndActiveTrue(UUID tenantId, String type);
}