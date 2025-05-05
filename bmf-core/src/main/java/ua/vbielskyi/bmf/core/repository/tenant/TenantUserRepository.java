package ua.vbielskyi.bmf.core.repository.tenant;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.tenant.TenantUserEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends MultiTenantJpaRepository<TenantUserEntity, UUID> {

    List<TenantUserEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    List<TenantUserEntity> findAllByTenantIdAndLocationId(UUID tenantId, UUID locationId);

    Optional<TenantUserEntity> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<TenantUserEntity> findByTenantIdAndTelegramId(UUID tenantId, Long telegramId);

    @Query("SELECT COUNT(u) FROM TenantUserEntity u WHERE u.tenantId = :tenantId AND u.role = :role")
    Long countByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);
}