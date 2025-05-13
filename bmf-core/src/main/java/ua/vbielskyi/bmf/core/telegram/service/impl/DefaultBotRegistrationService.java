package ua.vbielskyi.bmf.core.telegram.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ua.vbielskyi.bmf.core.telegram.exception.BotRegistrationException;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotRegistrationService;

import java.util.UUID;

/**
 * Implementation of BotRegistrationService that uses Telegram API
 * and caches bot configurations in Redis
 */
@Slf4j
@Service
public class DefaultBotRegistrationService implements BotRegistrationService {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1 second

    private final RestTemplate restTemplate;
    private final CachedBotRegistry botRegistry;

    public DefaultBotRegistrationService(RestTemplate restTemplate, CachedBotRegistry botRegistry) {
        this.restTemplate = restTemplate;
        this.botRegistry = botRegistry;
    }

    @Override
    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class},
            maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = RETRY_DELAY, multiplier = 2)
    )
    public boolean registerBot(BotType botType, String token, String username, String webhookUrl, UUID tenantId,
                               Long cacheExpirationSeconds) {
        try {
            log.info("Registering bot: {}, tenant: {}", username, tenantId);

            validateBotToken(token);
            validateWebhookUrl(webhookUrl);

            // Set webhook with Telegram
            boolean success = setWebhook(token, webhookUrl);

            if (success) {
                // Store in registry cache
                botRegistry.registerBot(botType, token, username, webhookUrl, tenantId, cacheExpirationSeconds);

                log.info("Successfully registered bot: {}, tenant: {}", username, tenantId);
            } else {
                log.error("Failed to set webhook for bot: {}, tenant: {}", username, tenantId);
            }

            return success;
        } catch (HttpClientErrorException e) {
            log.error("Telegram API client error: {}", e.getMessage(), e);
            throw new BotRegistrationException("Telegram API client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            log.error("Telegram API server error: {}", e.getMessage(), e);
            throw new BotRegistrationException("Telegram API server error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error registering bot: {}, tenant: {}", username, tenantId, e);
            throw new BotRegistrationException("Error registering bot: " + e.getMessage(), e);
        }
    }

    @Override
    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class},
            maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = RETRY_DELAY, multiplier = 2)
    )
    public boolean unregisterBot(String token, UUID tenantId) {
        try {
            validateBotToken(token);

            // Delete webhook with Telegram
            boolean success = deleteWebhook(token);

            if (success) {
                // Remove from registry cache
                botRegistry.unregisterBot(token, tenantId);

                log.info("Successfully unregistered bot, tenant: {}", tenantId);
            } else {
                log.error("Failed to delete webhook for bot, tenant: {}", tenantId);
            }

            return success;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Telegram API error: {}", e.getMessage(), e);
            throw new BotRegistrationException("Telegram API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error unregistering bot, tenant: {}", tenantId, e);
            throw new BotRegistrationException("Error unregistering bot: " + e.getMessage(), e);
        }
    }

    @Override
    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class},
            maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = RETRY_DELAY, multiplier = 2)
    )
    public boolean updateWebhook(String token, String newWebhookUrl, UUID tenantId) {
        try {
            validateBotToken(token);
            validateWebhookUrl(newWebhookUrl);

            // Get bot config
            CachedBotRegistry.BotConfig config = botRegistry.getBotConfigByToken(token);

            if (config == null) {
                log.warn("No bot configuration found for token: {}", token);
                return false;
            }

            // Update webhook with Telegram
            boolean success = setWebhook(token, newWebhookUrl);

            if (success) {
                // Update the registry with new webhook URL
                botRegistry.registerBot(
                        config.getBotType(),
                        config.getToken(),
                        config.getUsername(),
                        newWebhookUrl,
                        config.getTenantId(),
                        86400L
                );

                log.info("Successfully updated webhook for bot, tenant: {}", tenantId);
            } else {
                log.error("Failed to update webhook for bot, tenant: {}", tenantId);
            }

            return success;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Telegram API error: {}", e.getMessage(), e);
            throw new BotRegistrationException("Telegram API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating webhook for bot, tenant: {}", tenantId, e);
            throw new BotRegistrationException("Error updating webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Set webhook for a bot via Telegram API
     */
    private boolean setWebhook(String token, String webhookUrl) {
        try {
            String url = TELEGRAM_API_URL + token + "/setWebhook";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            String requestBody = "{\"url\":\"" + webhookUrl + "\",\"max_connections\":40,\"allowed_updates\":[\"message\",\"callback_query\",\"inline_query\"]}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (!success) {
                log.error("Failed to set webhook. Response: {}", response.getBody());
            }
            return success;
        } catch (Exception e) {
            log.error("Error setting webhook", e);
            throw new BotRegistrationException("Error setting webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Delete webhook for a bot via Telegram API
     */
    private boolean deleteWebhook(String token) {
        try {
            String url = TELEGRAM_API_URL + token + "/deleteWebhook";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (!success) {
                log.error("Failed to delete webhook. Response: {}", response.getBody());
            }
            return success;
        } catch (Exception e) {
            log.error("Error deleting webhook", e);
            throw new BotRegistrationException("Error deleting webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Validate bot token format
     */
    private void validateBotToken(String token) {
        if (token == null || !token.matches("^\\d+:AA[A-Za-z0-9_-]{31,}$")) {
            throw new BotRegistrationException("Invalid bot token format");
        }
    }

    /**
     * Validate webhook URL
     */
    private void validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || !webhookUrl.startsWith("https://")) {
            throw new BotRegistrationException("Webhook URL must start with https://");
        }
    }
}