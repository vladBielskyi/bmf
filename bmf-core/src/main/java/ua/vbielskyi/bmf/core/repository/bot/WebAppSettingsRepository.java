package ua.vbielskyi.bmf.core.repository.bot;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.bot.WebAppSettingsEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebAppSettingsRepository extends MultiTenantJpaRepository<WebAppSettingsEntity, UUID> {

    Optional<WebAppSettingsEntity> findByTenantId(UUID tenantId);
}