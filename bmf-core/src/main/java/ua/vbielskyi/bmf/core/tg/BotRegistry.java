package ua.vbielskyi.bmf.core.tg;

import java.util.List;
import java.util.UUID;
import ua.vbielskyi.bmf.core.tg.handler.BotUpdateHandler;
import ua.vbielskyi.bmf.core.tg.model.BotType;

/**
 * Registry for bot handlers
 */
public interface BotRegistry {
    /**
     * Register a bot update handler
     *
     * @param handler The handler to register
     */
    void registerHandler(BotUpdateHandler handler);

    /**
     * Find a handler that can handle updates for this bot type and tenant
     *
     * @param botType The bot type (admin or tenant)
     * @param tenantId Tenant ID (null for admin bot)
     * @return Handler or null if none found
     */
    BotUpdateHandler findHandler(BotType botType, UUID tenantId);

    /**
     * Get all registered handlers
     *
     * @return List of handlers
     */
    List<BotUpdateHandler> getAllHandlers();
}