package ua.vbielskyi.bmf.core.tenant.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extension of JpaRepository that automatically filters data by tenant ID
 */
@NoRepositoryBean
public interface MultiTenantJpaRepository<T extends TenantAware, ID extends Serializable> extends JpaRepository<T, ID> {

    /**
     * Get the current tenant ID from context
     */
    default UUID getCurrentTenant() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the current context");
        }
        return tenantId;
    }

    /**
     * Find all entities for the current tenant
     */
    @Override
    default List<T> findAll() {
        return findAllByTenantId(getCurrentTenant());
    }

    /**
     * Find all entities for the current tenant with sorting
     */
    @Override
    default List<T> findAll(Sort sort) {
        return findAllByTenantId(getCurrentTenant(), sort);
    }

    /**
     * Find all entities for the current tenant with pagination
     */
    @Override
    default Page<T> findAll(Pageable pageable) {
        return findAllByTenantId(getCurrentTenant(), pageable);
    }

    /**
     * Find entity by ID for current tenant
     */
    @Override
    default Optional<T> findById(ID id) {
        return findByIdAndTenantId(id, getCurrentTenant());
    }

    /**
     * Custom methods to be implemented by concrete repositories
     */
    List<T> findAllByTenantId(UUID tenantId);

    List<T> findAllByTenantId(UUID tenantId, Sort sort);

    Page<T> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<T> findByIdAndTenantId(ID id, UUID tenantId);
}