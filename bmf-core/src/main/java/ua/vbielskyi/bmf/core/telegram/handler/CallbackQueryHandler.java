package ua.vbielskyi.bmf.core.telegram.handler;

import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;

/**
 * Interface for handling callback queries
 */
public interface CallbackQueryHandler {

    /**
     * Get the callback data prefix this handler can process
     *
     * @return Callback data prefix
     */
    String getCallbackPrefix();

    /**
     * Handle a callback query
     *
     * @param message Bot message containing the callback query
     * @return Response to send back
     */
    BotResponse handle(BotMessage message);
}