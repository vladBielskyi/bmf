package ua.vbielskyi.bmf.core.repository.bot;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.bot.BotSettingsEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotSettingsRepository extends MultiTenantJpaRepository<BotSettingsEntity, UUID> {

    Optional<BotSettingsEntity> findByTenantId(UUID tenantId);
}