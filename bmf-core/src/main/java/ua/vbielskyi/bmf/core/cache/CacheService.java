package ua.vbielskyi.bmf.core.cache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Interface for caching operations
 */
public interface CacheService {

    /**
     * Put a value in the cache
     * @param key Cache key
     * @param value Value to cache
     * @param tenantId Tenant ID
     * @param <T> Value type
     */
    <T> void put(String key, T value, UUID tenantId);

    /**
     * Put a value in the cache with expiration
     * @param key Cache key
     * @param value Value to cache
     * @param expiration Expiration time
     * @param timeUnit Time unit for expiration
     * @param tenantId Tenant ID
     * @param <T> Value type
     */
    <T> void put(String key, T value, long expiration, TimeUnit timeUnit, UUID tenantId);

    /**
     * Get a value from the cache
     * @param key Cache key
     * @param type Value type class
     * @param tenantId Tenant ID
     * @param <T> Value type
     * @return Optional containing the value if found
     */
    <T> Optional<T> get(String key, Class<T> type, UUID tenantId);

    /**
     * Get a value from the cache or compute it if not present
     * @param key Cache key
     * @param type Value type class
     * @param supplier Supplier to compute value if not in cache
     * @param tenantId Tenant ID
     * @param <T> Value type
     * @return Cached or computed value
     */
    <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, UUID tenantId);

    /**
     * Get a value from the cache or compute it if not present, with expiration
     * @param key Cache key
     * @param type Value type class
     * @param supplier Supplier to compute value if not in cache
     * @param expiration Expiration time
     * @param timeUnit Time unit for expiration
     * @param tenantId Tenant ID
     * @param <T> Value type
     * @return Cached or computed value
     */
    <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, long expiration, TimeUnit timeUnit, UUID tenantId);

    /**
     * Remove a value from the cache
     * @param key Cache key
     * @param tenantId Tenant ID
     */
    void remove(String key, UUID tenantId);

    /**
     * Remove all values for a tenant
     * @param tenantId Tenant ID
     */
    void removeAll(UUID tenantId);

    /**
     * Remove values by pattern
     * @param keyPattern Key pattern
     * @param tenantId Tenant ID
     */
    void removeByPattern(String keyPattern, UUID tenantId);

    /**
     * Get all keys
     * @param tenantId Tenant ID
     * @return List of keys
     */
    List<String> getKeys(UUID tenantId);

    /**
     * Check if a key exists
     * @param key Cache key
     * @param tenantId Tenant ID
     * @return True if key exists, false otherwise
     */
    boolean exists(String key, UUID tenantId);
}