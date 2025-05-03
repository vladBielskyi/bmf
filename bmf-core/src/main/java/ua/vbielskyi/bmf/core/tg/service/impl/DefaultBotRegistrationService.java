package ua.vbielskyi.bmf.core.tg.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.tg.service.BotRegistrationService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DefaultBotRegistrationService implements BotRegistrationService {

    private final Map<String, TelegramApiClient> botClients = new ConcurrentHashMap<>();

    @Override
    public boolean registerBot(String token, String username, String webhookUrl, UUID tenantId) {
        try {
            log.info("Registering bot: {}, tenant: {}", username, tenantId);

            // Create a client for this bot
            TelegramApiClient client = new TelegramApiClient(token);

            // Set webhook
            boolean success = client.setWebhook(webhookUrl);

            if (success) {
                // Store client for later use
                botClients.put(token, client);
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
            TelegramApiClient client = botClients.get(token);

            if (client == null) {
                // Create a temporary client
                client = new TelegramApiClient(token);
            }

            // Delete webhook
            boolean success = client.deleteWebhook();

            if (success) {
                // Remove client
                botClients.remove(token);
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
            TelegramApiClient client = botClients.get(token);

            if (client == null) {
                // Create a client for this bot
                client = new TelegramApiClient(token);
                botClients.put(token, client);
            }

            // Set new webhook
            boolean success = client.setWebhook(newWebhookUrl);

            if (success) {
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

    private static class TelegramApiClient {
        private final String token;

        public TelegramApiClient(String token) {
            this.token = token;
        }

        public boolean setWebhook(String url) {
            // Make API call to Telegram to set webhook
            // This is a simplified implementation
            try {
                // In a real implementation, you would use the Telegram API client
                // Here we just simulate a successful response
                log.info("Setting webhook to {}", url);
                return true;
            } catch (Exception e) {
                log.error("Error setting webhook", e);
                return false;
            }
        }

        public boolean deleteWebhook() {
            // Make API call to Telegram to delete webhook
            // This is a simplified implementation
            try {
                // In a real implementation, you would use the Telegram API client
                // Here we just simulate a successful response
                log.info("Deleting webhook");
                return true;
            } catch (Exception e) {
                log.error("Error deleting webhook", e);
                return false;
            }
        }
    }
}
