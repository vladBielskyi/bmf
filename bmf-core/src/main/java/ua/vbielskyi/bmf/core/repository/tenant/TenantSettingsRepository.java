package ua.vbielskyi.bmf.core.repository.tenant;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.tenant.TenantSettingsEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSettingsRepository extends MultiTenantJpaRepository<TenantSettingsEntity, UUID> {

    Optional<TenantSettingsEntity> findByTenantId(UUID tenantId);
}