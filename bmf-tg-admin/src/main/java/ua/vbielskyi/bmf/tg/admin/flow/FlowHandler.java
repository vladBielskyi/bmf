package ua.vbielskyi.bmf.tg.admin.flow;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;

/**
 * Interface for handling flow-specific updates
 */
public interface FlowHandler {

    /**
     * Handle an update within a specific flow
     *
     * @param update The Telegram update
     * @param session The user's session
     * @return Response to send back to user
     */
    BotApiMethod<?> handleUpdate(Update update, UserSession session);
}