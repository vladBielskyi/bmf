package ua.vbielskyi.bmf.core.repository.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.tenant.TenantOwnerEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantOwnerRepository extends JpaRepository<TenantOwnerEntity, UUID> {

    List<TenantOwnerEntity> findAllByUserId(Long userId);

    List<TenantOwnerEntity> findAllByTenantId(UUID tenantId);

    boolean existsByUserIdAndTenantId(Long userId, UUID tenantId);

    @Query("SELECT to.tenantId FROM TenantOwnerEntity to WHERE to.userId = :userId")
    List<UUID> findTenantIdsByUserId(@Param("userId") Long userId);
}