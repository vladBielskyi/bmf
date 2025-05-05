package ua.vbielskyi.bmf.core.tg.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;
import ua.vbielskyi.bmf.core.tg.BotRegistry;
import ua.vbielskyi.bmf.core.tg.handler.BotUpdateHandler;
import ua.vbielskyi.bmf.core.tg.model.BotType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CachedBotRegistry implements BotRegistry {

    private static final String BOT_CONFIG_KEY_PREFIX = "bmf:bot:config:";
    private static final String BOT_TOKEN_KEY_PREFIX = "bmf:bot:token:";
    private static final String TENANT_BOTS_KEY = "bmf:tenant:bots";
    private static final long CACHE_EXPIRATION = 24 * 60 * 60; // 24 hours

    private final List<BotUpdateHandler> handlers = new CopyOnWriteArrayList<>();
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public CachedBotRegistry(CacheService cacheService, ObjectMapper objectMapper) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerHandler(BotUpdateHandler handler) {
        handlers.add(handler);
        log.info("Registered bot handler: {}", handler.getBotType());
    }

    @Override
    public BotUpdateHandler findHandler(BotType botType, UUID tenantId) {
        // First check if bot is registered and active for tenant bots
        if (tenantId != null) {
            BotConfig config = getBotConfig(botType, tenantId);
            if (config == null || !config.isActive()) {
                log.warn("No active bot configuration found for tenant: {}", tenantId);
                return null;
            }
        }

        // Find handler that can handle this request
        for (BotUpdateHandler handler : handlers) {
            if (handler.canHandle(botType, tenantId)) {
                return handler;
            }
        }

        log.warn("No handler found for botType: {}, tenantId: {}", botType, tenantId);
        return null;
    }

    @Override
    public List<BotUpdateHandler> getAllHandlers() {
        return new ArrayList<>(handlers);
    }

    /**
     * Register a bot with the registry
     *
     * @param botType Bot type (admin or tenant)
     * @param token Bot token
     * @param username Bot username
     * @param webhookUrl Webhook URL
     * @param tenantId Tenant ID (null for admin bot)
     * @return true if registration was successful
     */
    public boolean registerBot(BotType botType, String token, String username, String webhookUrl, UUID tenantId) {
        try {
            BotConfig config = new BotConfig(botType, token, username, webhookUrl, tenantId);
            String json = objectMapper.writeValueAsString(config);

            // Store in cache
            String configKey = getBotConfigKey(botType, tenantId);
            String tokenKey = BOT_TOKEN_KEY_PREFIX + token;

            cacheService.put(configKey, json, CACHE_EXPIRATION, TimeUnit.SECONDS);
            cacheService.put(tokenKey, json, CACHE_EXPIRATION, TimeUnit.SECONDS);

            // For tenant bots, add to the list of tenant bots
            if (tenantId != null) {
                updateTenantsList(tenantId, true);
            }

            log.info("Registered bot: {} for tenant: {}", username, tenantId);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bot config", e);
            return false;
        }
    }

    /**
     * Unregister a bot from the registry
     *
     * @param token Bot token
     * @param tenantId Tenant ID (null for admin bot)
     * @return true if unregistration was successful
     */
    public boolean unregisterBot(String token, UUID tenantId) {
        // Get bot config by token
        BotConfig config = getBotConfigByToken(token);
        if (config == null) {
            log.warn("No bot configuration found for token: {}", token);
            return false;
        }

        // Remove from cache
        String configKey = getBotConfigKey(config.getBotType(), config.getTenantId());
        String tokenKey = BOT_TOKEN_KEY_PREFIX + token;

        cacheService.remove(configKey);
        cacheService.remove(tokenKey);

        // For tenant bots, remove from the list of tenant bots
        if (config.getTenantId() != null) {
            updateTenantsList(config.getTenantId(), false);
        }

        log.info("Unregistered bot for tenant: {}", tenantId);
        return true;
    }

    /**
     * Update a bot's active status
     *
     * @param botType Bot type
     * @param tenantId Tenant ID (null for admin bot)
     * @param active Active status
     * @return true if update was successful
     */
    public boolean updateBotActiveStatus(BotType botType, UUID tenantId, boolean active) {
        BotConfig config = getBotConfig(botType, tenantId);
        if (config == null) {
            log.warn("No bot configuration found for botType: {}, tenantId: {}", botType, tenantId);
            return false;
        }

        config.setActive(active);

        try {
            String json = objectMapper.writeValueAsString(config);
            String configKey = getBotConfigKey(botType, tenantId);
            String tokenKey = BOT_TOKEN_KEY_PREFIX + config.getToken();

            cacheService.put(configKey, json, CACHE_EXPIRATION, TimeUnit.SECONDS);
            cacheService.put(tokenKey, json, CACHE_EXPIRATION, TimeUnit.SECONDS);

            log.info("Updated bot active status to {} for botType: {}, tenantId: {}",
                    active, botType, tenantId);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bot config", e);
            return false;
        }
    }

    /**
     * Get a bot configuration by bot type and tenant ID
     *
     * @param botType Bot type
     * @param tenantId Tenant ID
     * @return Bot configuration or null if not found
     */
    public BotConfig getBotConfig(BotType botType, UUID tenantId) {
        String configKey = getBotConfigKey(botType, tenantId);
        return cacheService.get(configKey, String.class)
                .map(this::deserializeBotConfig)
                .orElse(null);
    }

    /**
     * Get a bot configuration by token
     *
     * @param token Bot token
     * @return Bot configuration or null if not found
     */
    public BotConfig getBotConfigByToken(String token) {
        String tokenKey = BOT_TOKEN_KEY_PREFIX + token;
        return cacheService.get(tokenKey, String.class)
                .map(this::deserializeBotConfig)
                .orElse(null);
    }

    /**
     * Get all tenant bot configurations
     *
     * @return Map of tenant IDs to bot configurations
     */
    public Map<UUID, BotConfig> getAllTenantBotConfigs() {
        Map<UUID, BotConfig> result = new HashMap<>();

        // Get the list of tenant IDs
        cacheService.get(TENANT_BOTS_KEY, String.class)
                .map(this::deserializeTenantsList)
                .orElse(new ArrayList<>())
                .forEach(tenantId -> {
                    BotConfig config = getBotConfig(BotType.TENANT, tenantId);
                    if (config != null) {
                        result.put(tenantId, config);
                    }
                });

        return result;
    }

    /**
     * Update the list of tenant bots
     *
     * @param tenantId Tenant ID
     * @param add Whether to add or remove
     */
    private void updateTenantsList(UUID tenantId, boolean add) {
        List<UUID> tenantIds = cacheService.get(TENANT_BOTS_KEY, String.class)
                .map(this::deserializeTenantsList)
                .orElse(new ArrayList<>());

        if (add && !tenantIds.contains(tenantId)) {
            tenantIds.add(tenantId);
        } else if (!add) {
            tenantIds.remove(tenantId);
        } else {
            return; // No change needed
        }

        try {
            String json = objectMapper.writeValueAsString(tenantIds);
            cacheService.put(TENANT_BOTS_KEY, json, CACHE_EXPIRATION, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tenant list", e);
        }
    }

    /**
     * Deserialize a bot configuration from JSON
     *
     * @param json JSON string
     * @return Bot configuration or null if deserialization fails
     */
    private BotConfig deserializeBotConfig(String json) {
        try {
            return objectMapper.readValue(json, BotConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize bot config", e);
            return null;
        }
    }

    /**
     * Deserialize a list of tenant IDs from JSON
     *
     * @param json JSON string
     * @return List of tenant IDs or empty list if deserialization fails
     */
    private List<UUID> deserializeTenantsList(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, UUID.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize tenant list", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get the bot configuration cache key
     *
     * @param botType Bot type
     * @param tenantId Tenant ID
     * @return Cache key
     */
    private String getBotConfigKey(BotType botType, UUID tenantId) {
        if (tenantId == null) {
            return BOT_CONFIG_KEY_PREFIX + botType;
        } else {
            return BOT_CONFIG_KEY_PREFIX + botType + ":" + tenantId;
        }
    }

    /**
     * Bot configuration class
     */
    @Setter
    @Getter
    public static class BotConfig {
        private BotType botType;
        private String token;
        private String username;
        private String webhookUrl;
        private UUID tenantId;
        private boolean active = true;

        public BotConfig() {
            // Default constructor for Jackson
        }

        public BotConfig(BotType botType, String token, String username, String webhookUrl, UUID tenantId) {
            this.botType = botType;
            this.token = token;
            this.username = username;
            this.webhookUrl = webhookUrl;
            this.tenantId = tenantId;
        }

    }
}