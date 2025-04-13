package ua.vbielskyi.bmf.core.tenant.entity;

import java.util.UUID;

/**
 * Interface for entities that belong to a tenant
 */
public interface TenantAware {

    /**
     * Get the tenant ID
     * @return tenant ID
     */
    UUID getTenantId();

    /**
     * Set the tenant ID
     * @param tenantId tenant ID
     */
    void setTenantId(UUID tenantId);
}