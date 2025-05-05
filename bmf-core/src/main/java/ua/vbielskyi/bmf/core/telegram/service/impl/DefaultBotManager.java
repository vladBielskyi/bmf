package ua.vbielskyi.bmf.core.telegram.service.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ua.vbielskyi.bmf.core.telegram.BotManager;

@Slf4j
public abstract class DefaultBotManager implements BotManager {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    private final Map<String, BotInfo> registeredBots = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    public DefaultBotManager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean registerBot(String token, String username, String webhookUrl, UUID tenantId) {
        try {
            log.info("Registering bot: {}, tenant: {}", username, tenantId);

            // Set webhook
            boolean success = setWebhook(token, webhookUrl);

            if (success) {
                // Store bot info
                registeredBots.put(token, new BotInfo(username, tenantId, webhookUrl));
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
            boolean success = deleteWebhook(token);

            if (success) {
                registeredBots.remove(token);
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
            boolean success = setWebhook(token, newWebhookUrl);

            if (success) {
                BotInfo info = registeredBots.get(token);
                if (info != null) {
                    info.webhookUrl = newWebhookUrl;
                } else {
                    registeredBots.put(token, new BotInfo(null, tenantId, newWebhookUrl));
                }
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

    @Override
    public boolean isBotRegistered(String token) {
        return registeredBots.containsKey(token);
    }

    protected boolean setWebhook(String token, String webhookUrl) {
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

    protected boolean deleteWebhook(String token) {
        try {
            String url = TELEGRAM_API_URL + token + "/deleteWebhook";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting webhook", e);
            return false;
        }
    }

    private static class BotInfo {
        private final String username;
        private final UUID tenantId;
        private String webhookUrl;

        public BotInfo(String username, UUID tenantId, String webhookUrl) {
            this.username = username;
            this.tenantId = tenantId;
            this.webhookUrl = webhookUrl;
        }
    }
}