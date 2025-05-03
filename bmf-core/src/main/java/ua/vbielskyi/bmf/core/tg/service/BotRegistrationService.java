package ua.vbielskyi.bmf.core.tg.service;

import java.util.UUID;

/**
 * Core service interface for registering bots with Telegram
 */
public interface BotRegistrationService {

    /**
     * Register a bot with Telegram
     *
     * @param token Bot token
     * @param username Bot username
     * @param webhookUrl Webhook URL
     * @param tenantId Tenant ID (null for admin bot)
     * @return True if registration was successful
     */
    boolean registerBot(String token, String username, String webhookUrl, UUID tenantId);

    /**
     * Unregister a bot from Telegram
     *
     * @param token Bot token
     * @param tenantId Tenant ID (null for admin bot)
     * @return True if unregistration was successful
     */
    boolean unregisterBot(String token, UUID tenantId);

    /**
     * Update webhook URL for a bot
     *
     * @param token Bot token
     * @param newWebhookUrl New webhook URL
     * @param tenantId Tenant ID (null for admin bot)
     * @return True if update was successful
     */
    boolean updateWebhook(String token, String newWebhookUrl, UUID tenantId);
}