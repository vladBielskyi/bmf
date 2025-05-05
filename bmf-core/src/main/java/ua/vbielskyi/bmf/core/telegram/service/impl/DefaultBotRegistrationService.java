package ua.vbielskyi.bmf.core.telegram.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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

    private final RestTemplate restTemplate;
    private final CachedBotRegistry botRegistry;

    public DefaultBotRegistrationService(RestTemplate restTemplate, CachedBotRegistry botRegistry) {
        this.restTemplate = restTemplate;
        this.botRegistry = botRegistry;
    }

    @Override
    public boolean registerBot(BotType botType, String token, String username, String webhookUrl, UUID tenantId) {
        try {
            log.info("Registering bot: {}, tenant: {}", username, tenantId);

            // Set webhook with Telegram
            boolean success = setWebhook(token, webhookUrl);

            if (success) {

                // Store in registry cache
                botRegistry.registerBot(botType, token, username, webhookUrl, tenantId);

                log.info("Successfully registered bot: {}, tenant: {}", username, tenantId);
            } else {
                log.error("Failed to set webhook for bot: {}, tenant: {}", username, tenantId);
            }

            return success;
        } catch (Exception e) {
            log.error("Error registering bot: {}, tenant: {}", username, tenantId, e);
            return false;
        }
    }

    @Override
    public boolean unregisterBot(String token, UUID tenantId) {
        try {
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
        } catch (Exception e) {
            log.error("Error unregistering bot, tenant: {}", tenantId, e);
            return false;
        }
    }

    @Override
    public boolean updateWebhook(String token, String newWebhookUrl, UUID tenantId) {
        try {
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
                        config.getTenantId()
                );

                log.info("Successfully updated webhook for bot, tenant: {}", tenantId);
            } else {
                log.error("Failed to update webhook for bot, tenant: {}", tenantId);
            }

            return success;
        } catch (Exception e) {
            log.error("Error updating webhook for bot, tenant: {}", tenantId, e);
            return false;
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

            String requestBody = "{\"url\":\"" + webhookUrl + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error setting webhook", e);
            return false;
        }
    }

    /**
     * Delete webhook for a bot via Telegram API
     */
    private boolean deleteWebhook(String token) {
        try {
            String url = TELEGRAM_API_URL + token + "/deleteWebhook";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting webhook", e);
            return false;
        }
    }
}