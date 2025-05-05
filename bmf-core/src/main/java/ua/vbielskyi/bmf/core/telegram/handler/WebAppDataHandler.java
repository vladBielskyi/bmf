package ua.vbielskyi.bmf.core.telegram.handler;

import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;

/**
 * Interface for handling WebApp data
 */
public interface WebAppDataHandler {

    /**
     * Check if this handler can process the WebApp data
     *
     * @param message Bot message containing WebApp data
     * @return True if this handler can process the data
     */
    boolean canHandle(BotMessage message);

    /**
     * Handle WebApp data
     *
     * @param message Bot message containing WebApp data
     * @return Response to send back
     */
    BotResponse handle(BotMessage message);
}