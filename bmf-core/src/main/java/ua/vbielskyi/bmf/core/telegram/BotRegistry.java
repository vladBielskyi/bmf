package ua.vbielskyi.bmf.core.telegram;

import java.util.List;
import java.util.UUID;
import ua.vbielskyi.bmf.core.telegram.handler.BotHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotType;

/**
 * Registry for bot handlers
 */
public interface BotRegistry {
    /**
     * Register a bot update handler
     *
     * @param handler The handler to register
     */
    void registerHandler(BotHandler handler);

    /**
     * Find a handler that can handle updates for this bot type and tenant
     *
     * @param botType The bot type (admin or tenant)
     * @param tenantId Tenant ID (null for admin bot)
     * @return Handler or null if none found
     */
    BotHandler findHandler(BotType botType, UUID tenantId);

    /**
     * Get all registered handlers
     *
     * @return List of handlers
     */
    List<BotHandler> getAllHandlers();
}