package ua.vbielskyi.bmf.core.repository.tenant;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.tenant.TenantLocationEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantLocationRepository extends MultiTenantJpaRepository<TenantLocationEntity, UUID> {

    List<TenantLocationEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    List<TenantLocationEntity> findByTenantIdAndActiveTrue(UUID tenantId);
}