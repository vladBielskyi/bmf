package ua.vbielskyi.bmf.core.cache.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis implementation of CacheService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "bmf:cache:";

    @Override
    public <T> void put(String key, T value, UUID tenantId) {
        try {
            String cacheKey = buildKey(key, tenantId);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue);
            log.debug("Cached value for key: {}, tenant: {}", key, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for key: {}, tenant: {}", key, tenantId, e);
        }
    }

    @Override
    public <T> void put(String key, T value, long expiration, TimeUnit timeUnit, UUID tenantId) {
        try {
            String cacheKey = buildKey(key, tenantId);
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, expiration, timeUnit);
            log.debug("Cached value with expiration for key: {}, tenant: {}", key, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value with expiration for key: {}, tenant: {}", key, tenantId, e);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type, UUID tenantId) {
        try {
            String cacheKey = buildKey(key, tenantId);
            String value = redisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                return Optional.empty();
            }

            T result = objectMapper.readValue(value, type);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error retrieving value for key: {}, tenant: {}", key, tenantId, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrElseCompute(String key, Class<T> type, Supplier<T> supplier, UUID tenantId) {
        Optional<T> cachedValue = get(key, type, tenantId);

        if (cachedValue.isPresent()) {
            log.debug("Cache hit for key: {}, tenant: {}", key, tenantId);
            return cachedValue.get();
        }

        log.debug("Cache miss for key: {}, tenant: {}", key, tenantId);
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
            log.debug("Cache hit for key: {}, tenant: {}", key, tenantId);
            return cachedValue.get();
        }

        log.debug("Cache miss for key: {}, tenant: {}", key, tenantId);
        T computedValue = supplier.get();

        if (computedValue != null) {
            put(key, computedValue, expiration, timeUnit, tenantId);
        }

        return computedValue;
    }

    @Override
    public void remove(String key, UUID tenantId) {
        String cacheKey = buildKey(key, tenantId);
        redisTemplate.delete(cacheKey);
        log.debug("Removed cached value for key: {}, tenant: {}", key, tenantId);
    }

    @Override
    public void removeAll(UUID tenantId) {
        String pattern = CACHE_PREFIX + tenantId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed all cached values for tenant: {}, count: {}", tenantId, keys.size());
        }
    }

    @Override
    public void removeByPattern(String keyPattern, UUID tenantId) {
        String pattern = CACHE_PREFIX + tenantId + ":" + keyPattern;
        Set<String> keys = redisTemplate.keys(pattern);

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Removed cached values by pattern: {}, tenant: {}, count: {}",
                    keyPattern, tenantId, keys.size());
        }
    }

    @Override
    public List<String> getKeys(UUID tenantId) {
        String pattern = CACHE_PREFIX + tenantId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        return keys.stream()
                .map(key -> key.substring((CACHE_PREFIX + tenantId + ":").length()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String key, UUID tenantId) {
        String cacheKey = buildKey(key, tenantId);
        Boolean exists = redisTemplate.hasKey(cacheKey);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Build a tenant-specific cache key
     *
     * @param key      Base key
     * @param tenantId Tenant ID
     * @return Tenant-specific cache key
     */
    private String buildKey(String key, UUID tenantId) {
        return CACHE_PREFIX + tenantId + ":" + key;
    }
}