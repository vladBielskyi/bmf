package ua.vbielskyi.bmf.core.repository.bot;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.bot.TelegramSessionEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramSessionRepository extends MultiTenantJpaRepository<TelegramSessionEntity, UUID> {

    Optional<TelegramSessionEntity> findByTenantIdAndTelegramId(UUID tenantId, Long telegramId);

    List<TelegramSessionEntity> findAllByTenantIdAndLastActivityAtBefore(UUID tenantId, LocalDateTime cutoffTime);

    void deleteAllByTenantIdAndLastActivityAtBefore(UUID tenantId, LocalDateTime cutoffTime);
}