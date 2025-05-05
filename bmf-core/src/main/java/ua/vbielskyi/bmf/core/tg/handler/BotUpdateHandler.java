package ua.vbielskyi.bmf.core.tg.handler;

import java.util.UUID;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.tg.model.BotType;

/**
 * Interface for handling bot updates
 */
public interface BotUpdateHandler {
    /**
     * Process a Telegram update
     *
     * @param update The update to process
     * @param tenantId Optional tenant ID (null for admin bot)
     * @return Response to send back
     */
    BotApiMethod<?> handleUpdate(Update update, UUID tenantId);

    /**
     * Check if this handler can process the update
     *
     * @param botType Bot type identifier
     * @param tenantId Tenant ID (null for admin bot)
     * @return True if this handler can process the update
     */
    boolean canHandle(BotType botType, UUID tenantId);

    /**
     * Get the bot type this handler is for
     *
     * @return Bot type identifier
     */
    BotType getBotType();
}