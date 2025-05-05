package ua.vbielskyi.bmf.core.telegram.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.core.telegram.handler.BotHandler;
import ua.vbielskyi.bmf.core.telegram.handler.CallbackQueryHandler;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.handler.WebAppDataHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotMessageType;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base implementation of BotHandler
 */
@Slf4j
public abstract class AbstractBotHandler implements BotHandler {

    // Command handlers
    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();

    // Callback query handlers
    private final Map<String, CallbackQueryHandler> callbackHandlers = new HashMap<>();

    // WebApp data handlers
    private final List<WebAppDataHandler> webAppHandlers;

    protected AbstractBotHandler(List<CommandHandler> commandHandlers,
                                 List<CallbackQueryHandler> callbackHandlers,
                                 List<WebAppDataHandler> webAppHandlers) {
        // Register command handlers
        for (CommandHandler handler : commandHandlers) {
            this.commandHandlers.put(handler.getCommand(), handler);
        }

        // Register callback handlers
        for (CallbackQueryHandler handler : callbackHandlers) {
            this.callbackHandlers.put(handler.getCallbackPrefix(), handler);
        }

        this.webAppHandlers = webAppHandlers;
    }

    @Override
    public BotApiMethod<?> handleUpdate(Update update, UUID tenantId) {
        try {
            // Set tenant context for this thread
            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
            }

            // Convert the update to a BotMessage
            BotMessage message = BotMessage.fromUpdate(update, tenantId);

            // Process the message
            BotResponse response = handleMessage(message);

            // Return the primary response method
            return response.getMethod();

        } catch (Exception e) {
            log.error("Error processing update for tenant {}", tenantId, e);
            return createErrorResponse(extractChatId(update));
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }

    @Override
    public BotResponse handleMessage(BotMessage message) {
        try {
            // Handle different types of messages
            if (message.getType() == BotMessageType.COMMAND) {
                return handleCommand(message);
            } else if (message.getType() == BotMessageType.CALLBACK_QUERY) {
                return handleCallbackQuery(message);
            } else if (message.getType() == BotMessageType.WEBAPP_DATA) {
                return handleWebAppData(message);
            } else if (message.getType() == BotMessageType.TEXT) {
                return handleText(message);
            } else {
                return handleOtherMessageTypes(message);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            return BotResponse.text(message.getChatId(), "Sorry, something went wrong. Please try again later.");
        }
    }

    /**
     * Handle command messages
     */
    protected BotResponse handleCommand(BotMessage message) {
        String command = message.getCommand();
        CommandHandler handler = commandHandlers.get(command);

        if (handler != null) {
            // Check authentication if required
            if (handler.requiresAuthentication() && !isAuthenticated(message)) {
                return BotResponse.text(message.getChatId(), "You need to be authenticated to use this command.");
            }

            return handler.handle(message);
        } else {
            return BotResponse.text(message.getChatId(), "Unknown command: /" + command);
        }
    }

    /**
     * Handle callback queries
     */
    protected BotResponse handleCallbackQuery(BotMessage message) {
        String callbackData = message.getCallbackData();

        // Find the appropriate handler based on the callback prefix
        for (Map.Entry<String, CallbackQueryHandler> entry : callbackHandlers.entrySet()) {
            if (callbackData.startsWith(entry.getKey())) {
                return entry.getValue().handle(message);
            }
        }

        return BotResponse.text(message.getChatId(), "Unknown callback query");
    }

    /**
     * Handle WebApp data
     */
    protected BotResponse handleWebAppData(BotMessage message) {
        // Find the appropriate handler based on the WebApp data
        for (WebAppDataHandler handler : webAppHandlers) {
            if (handler.canHandle(message)) {
                return handler.handle(message);
            }
        }

        return BotResponse.text(message.getChatId(), "Unknown WebApp data");
    }

    /**
     * Handle text messages
     */
    protected abstract BotResponse handleText(BotMessage message);

    /**
     * Handle other message types (photos, documents, etc.)
     */
    protected abstract BotResponse handleOtherMessageTypes(BotMessage message);

    /**
     * Check if the user is authenticated
     */
    protected abstract boolean isAuthenticated(BotMessage message);

    /**
     * Create an error response
     */
    private SendMessage createErrorResponse(Long chatId) {
        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Sorry, something went wrong. Please try again later.")
                .build();
    }

    /**
     * Extract chat ID from an update
     */
    private Long extractChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }
}