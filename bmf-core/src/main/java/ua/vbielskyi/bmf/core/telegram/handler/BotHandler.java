package ua.vbielskyi.bmf.core.telegram.handler;

import java.util.UUID;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;

/**
 * Interface for handling bot updates
 */
public interface BotHandler {
    /**
     * Process a Telegram update and return a response
     *
     * @param update The update to process
     * @param tenantId Tenant ID (null for admin bot)
     * @return Response to send back
     */
    BotApiMethod<?> handleUpdate(Update update, UUID tenantId);

    /**
     * Process a BotMessage and return a BotResponse
     *
     * @param message The processed bot message
     * @return Response to send back
     */
    BotResponse handleMessage(BotMessage message);

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