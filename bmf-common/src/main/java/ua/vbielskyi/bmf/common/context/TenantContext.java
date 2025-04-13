package ua.vbielskyi.bmf.common.context;

import java.util.UUID;

/**
 * Thread-local context for storing the current tenant ID
 */
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the current tenant ID for this thread
     * @param tenantId The tenant ID
     */
    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread
     * @return The tenant ID or null if not set
     */
    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear the current tenant ID from this thread
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}