package ua.vbielskyi.bmf.core.telegram.handler;

import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;

/**
 * Interface for handling bot commands
 */
public interface CommandHandler {

    /**
     * Get the command this handler can process
     *
     * @return Command name without the /
     */
    String getCommand();

    /**
     * Handle a command message
     *
     * @param message Bot message containing the command
     * @return Response to send back
     */
    BotResponse handle(BotMessage message);

    /**
     * Check if this handler requires authentication
     *
     * @return True if authentication is required
     */
    default boolean requiresAuthentication() {
        return false;
    }
}