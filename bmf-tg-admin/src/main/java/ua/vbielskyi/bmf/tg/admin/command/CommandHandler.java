package ua.vbielskyi.bmf.tg.admin.command;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;

/**
 * Interface for handling specific commands
 */
public interface CommandHandler {

    /**
     * Handle the command
     *
     * @param update The Telegram update
     * @param session The user's session
     * @return Response to send back to user
     */
    BotApiMethod<?> handle(Update update, UserSession session);
}