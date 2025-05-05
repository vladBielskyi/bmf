package ua.vbielskyi.bmf.core.cache.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis implementation of CacheService with support for both
 * tenant-specific and general caching
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TENANT_CACHE_PREFIX = "bmf:tenant:cache:";
    private static final String GENERAL_CACHE_PREFIX = "bmf:general:cache:";

    // ========== Tenant-specific cache operations ==========

    @Override
    public <T> void put(String key, T value, UUID tenantId) {
        try {
            String cacheKey = buildTenantKey(key, tenantId);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue);
            log.debug("Cached value for tenant key: {}, tenant: {}", key, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for tenant key: {}, tenant: {}", key, tenantId, e);
        }
    }

    @Override
    public <T> void put(String key, T value, long expiration, TimeUnit timeUnit, UUID tenantId) {
        try {
            String cacheKey = buildTenantKey(key, tenantId);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, expiration, timeUnit);
            log.debug("Cached value with expiration for tenant key: {}, tenant: {}", key, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value with expiration for tenant key: {}, tenant: {}", key, tenantId, e);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type, UUID tenantId) {
        try {
            String cacheKey = buildTenantKey(key, tenantId);
            String value = redisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                return Optional.empty();
            }

            T result = objectMapper.readValue(value, type);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error retrieving value for tenant key: {}, tenant: {}", key, tenantId, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, UUID tenantId) {
        Optional<T> cachedValue = get(key, type, tenantId);

        if (cachedValue.isPresent()) {
            log.debug("Cache hit for tenant key: {}, tenant: {}", key, tenantId);
            return cachedValue.get();
        }

        log.debug("Cache miss for tenant key: {}, tenant: {}", key, tenantId);
        T computedValue = supplier.get();

        if (computedValue != null) {
            put(key, computedValue, tenantId);
        }

        return computedValue;
    }

    @Override
    public <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, long expiration, TimeUnit timeUnit, UUID tenantId) {
        Optional<T> cachedValue = get(key, type, tenantId);

        if (cachedValue.isPresent()) {
            log.debug("Cache hit for tenant key: {}, tenant: {}", key, tenantId);
            return cachedValue.get();
        }

        log.debug("Cache miss for tenant key: {}, tenant: {}", key, tenantId);
        T computedValue = supplier.get();

        if (computedValue != null) {
            put(key, computedValue, expiration, timeUnit, tenantId);
        }

        return computedValue;
    }

    @Override
    public void remove(String key, UUID tenantId) {
        String cacheKey = buildTenantKey(key, tenantId);
        redisTemplate.delete(cacheKey);
        log.debug("Removed cached value for tenant key: {}, tenant: {}", key, tenantId);
    }

    @Override
    public void removeAll(UUID tenantId) {
        String pattern = TENANT_CACHE_PREFIX + tenantId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed all cached values for tenant: {}, count: {}", tenantId, keys.size());
        }
    }

    @Override
    public void removeByPattern(String keyPattern, UUID tenantId) {
        String pattern = TENANT_CACHE_PREFIX + tenantId + ":" + keyPattern;
        Set<String> keys = redisTemplate.keys(pattern);

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed cached values by pattern: {}, tenant: {}, count: {}",
                    keyPattern, tenantId, keys.size());
        }
    }

    @Override
    public List<String> getKeys(UUID tenantId) {
        String pattern = TENANT_CACHE_PREFIX + tenantId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        return keys.stream()
                .map(key -> key.substring((TENANT_CACHE_PREFIX + tenantId + ":").length()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String key, UUID tenantId) {
        String cacheKey = buildTenantKey(key, tenantId);
        return redisTemplate.hasKey(cacheKey);
    }

    // ========== General (non-tenant-specific) cache operations ==========

    @Override
    public <T> void put(String key, T value) {
        try {
            String cacheKey = buildGeneralKey(key);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue);
            log.debug("Cached value for general key: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for general key: {}", key, e);
        }
    }

    @Override
    public <T> void put(String key, T value, long expiration, TimeUnit timeUnit) {
        try {
            String cacheKey = buildGeneralKey(key);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, expiration, timeUnit);
            log.debug("Cached value with expiration for general key: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value with expiration for general key: {}", key, e);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String cacheKey = buildGeneralKey(key);
            String value = redisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                return Optional.empty();
            }

            T result = objectMapper.readValue(value, type);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error retrieving value for general key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier) {
        Optional<T> cachedValue = get(key, type);

        if (cachedValue.isPresent()) {
            log.debug("Cache hit for general key: {}", key);
            return cachedValue.get();
        }

        log.debug("Cache miss for general key: {}", key);
        T computedValue = supplier.get();

        if (computedValue != null) {
            put(key, computedValue);
        }

        return computedValue;
    }

    @Override
    public <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, long expiration, TimeUnit timeUnit) {
        Optional<T> cachedValue = get(key, type);

        if (cachedValue.isPresent()) {
            log.debug("Cache hit for general key: {}", key);
            return cachedValue.get();
        }

        log.debug("Cache miss for general key: {}", key);
        T computedValue = supplier.get();

        if (computedValue != null) {
            put(key, computedValue, expiration, timeUnit);
        }

        return computedValue;
    }

    @Override
    public void remove(String key) {
        String cacheKey = buildGeneralKey(key);
        redisTemplate.delete(cacheKey);
        log.debug("Removed cached value for general key: {}", key);
    }

    @Override
    public void removeAll() {
        String pattern = GENERAL_CACHE_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed all general cached values, count: {}", keys.size());
        }
    }

    @Override
    public void removeByPattern(String keyPattern) {
        String pattern = GENERAL_CACHE_PREFIX + keyPattern;
        Set<String> keys = redisTemplate.keys(pattern);

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed general cached values by pattern: {}, count: {}", keyPattern, keys.size());
        }
    }

    @Override
    public List<String> getKeys() {
        String pattern = GENERAL_CACHE_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        return keys.stream()
                .map(key -> key.substring(GENERAL_CACHE_PREFIX.length()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String key) {
        String cacheKey = buildGeneralKey(key);
        Boolean exists = redisTemplate.hasKey(cacheKey);
        return Boolean.TRUE.equals(exists);
    }

    // ========== Helper methods ==========

    /**
     * Build a tenant-specific cache key
     *
     * @param key      Base key
     * @param tenantId Tenant ID
     * @return Tenant-specific cache key
     */
    private String buildTenantKey(String key, UUID tenantId) {
        return TENANT_CACHE_PREFIX + tenantId + ":" + key;
    }

    /**
     * Build a general (non-tenant-specific) cache key
     *
     * @param key Base key
     * @return General cache key
     */
    private String buildGeneralKey(String key) {
        return GENERAL_CACHE_PREFIX + key;
    }
}