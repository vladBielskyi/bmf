package ua.vbielskyi.bmf.core.telegram.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.core.telegram.exception.InvalidTelegramRequestException;
import ua.vbielskyi.bmf.core.telegram.exception.TelegramSessionNotFoundException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base implementation of BotHandler with improved error handling
 * and rate limiting
 */
@Slf4j
public abstract class AbstractBotHandler implements BotHandler {

    // Command handlers
    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();

    // Callback query handlers
    private final Map<String, CallbackQueryHandler> callbackHandlers = new HashMap<>();

    // WebApp data handlers
    private final List<WebAppDataHandler> webAppHandlers;

    // Simple rate limiting
    private final Map<Long, AtomicInteger> requestCounters = new ConcurrentHashMap<>();
    private final Map<Long, Long> requestTimestamps = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long ONE_MINUTE_MS = 60 * 1000;

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

            // Extract user ID for rate limiting
            Long userId = extractUserId(update);

            // Apply rate limiting
            if (userId != null && !checkRateLimit(userId)) {
                log.warn("Rate limit exceeded for user: {}", userId);
                return createRateLimitResponse(extractChatId(update));
            }

            // Convert the update to a BotMessage
            BotMessage message = BotMessage.fromUpdate(update, tenantId);

            if (message.getChatId() == null) {
                log.error("Chat ID is null, cannot process update");
                return null;
            }

            // Process the message
            BotResponse response = handleMessage(message);

            // Return the primary response method
            return response.getMethod();

        } catch (TelegramSessionNotFoundException e) {
            log.error("Session not found for tenant {}", tenantId, e);
            return createErrorResponse(extractChatId(update),
                    "Your session has expired. Please restart the conversation with /start.");
        } catch (InvalidTelegramRequestException e) {
            log.error("Invalid Telegram request for tenant {}", tenantId, e);
            return createErrorResponse(extractChatId(update),
                    "Invalid request. Please try again.");
        } catch (HttpClientErrorException e) {
            log.error("Telegram API error for tenant {}: {}", tenantId, e.getStatusCode(), e);
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return createErrorResponse(extractChatId(update),
                        "Telegram is processing too many requests. Please try again in a few moments.");
            } else {
                return createErrorResponse(extractChatId(update),
                        "There was a problem communicating with Telegram. Please try again later.");
            }
        } catch (Exception e) {
            log.error("Error processing update for tenant {}", tenantId, e);
            return createErrorResponse(extractChatId(update),
                    "Sorry, something went wrong. Please try again later.");
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
            return BotResponse.text(message.getChatId(),
                    "Sorry, something went wrong. Please try again later or use /help for available commands.");
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
                return BotResponse.text(message.getChatId(),
                        "You need to be authenticated to use this command. Please use /start to begin.");
            }

            try {
                return handler.handle(message);
            } catch (Exception e) {
                log.error("Error handling command '{}': {}", command, e.getMessage(), e);
                return BotResponse.text(message.getChatId(),
                        "Sorry, there was an error processing the /" + command + " command. Please try again later.");
            }
        } else {
            return BotResponse.text(message.getChatId(),
                    "Sorry, I don't recognize the command /" + command + ". Use /help to see available commands.");
        }
    }

    /**
     * Handle callback queries
     */
    protected BotResponse handleCallbackQuery(BotMessage message) {
        String callbackData = message.getCallbackData();
        if (callbackData == null || callbackData.isEmpty()) {
            log.warn("Empty callback data received");
            return BotResponse.text(message.getChatId(), "Invalid callback data received.");
        }

        // Find the appropriate handler based on the callback prefix
        for (Map.Entry<String, CallbackQueryHandler> entry : callbackHandlers.entrySet()) {
            if (callbackData.startsWith(entry.getKey())) {
                try {
                    return entry.getValue().handle(message);
                } catch (Exception e) {
                    log.error("Error handling callback query '{}': {}", callbackData, e.getMessage(), e);
                    return BotResponse.text(message.getChatId(),
                            "Sorry, there was an error processing your request. Please try again later.");
                }
            }
        }

        log.warn("No handler found for callback data: {}", callbackData);
        return BotResponse.text(message.getChatId(), "Sorry, I couldn't process this action. Please try again.");
    }

    /**
     * Handle WebApp data
     */
    protected BotResponse handleWebAppData(BotMessage message) {
        // Find the appropriate handler based on the WebApp data
        for (WebAppDataHandler handler : webAppHandlers) {
            if (handler.canHandle(message)) {
                try {
                    return handler.handle(message);
                } catch (Exception e) {
                    log.error("Error handling WebApp data: {}", e.getMessage(), e);
                    return BotResponse.text(message.getChatId(),
                            "Sorry, there was an error processing your WebApp data. Please try again later.");
                }
            }
        }

        log.warn("No handler found for WebApp data");
        return BotResponse.text(message.getChatId(),
                "Sorry, I couldn't process the WebApp data. Please try again or contact support.");
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
    private SendMessage createErrorResponse(Long chatId, String message) {
        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .build();
    }

    /**
     * Create a rate limit response
     */
    private SendMessage createRateLimitResponse(Long chatId) {
        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("You're sending too many requests. Please wait a moment before trying again.")
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

    /**
     * Extract user ID from an update for rate limiting
     */
    private Long extractUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        } else if (update.hasInlineQuery()) {
            return update.getInlineQuery().getFrom().getId();
        }
        return null;
    }

    /**
     * Check rate limit for a user
     *
     * @return true if within limit, false if exceeded
     */
    private boolean checkRateLimit(Long userId) {
        long currentTime = System.currentTimeMillis();

        // Reset counter if it's been more than a minute
        Long lastRequestTime = requestTimestamps.get(userId);
        if (lastRequestTime == null || currentTime - lastRequestTime > ONE_MINUTE_MS) {
            requestCounters.put(userId, new AtomicInteger(1));
            requestTimestamps.put(userId, currentTime);
            return true;
        }

        // Increment counter and check limit
        AtomicInteger counter = requestCounters.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        // Update timestamp
        requestTimestamps.put(userId, currentTime);

        return count <= MAX_REQUESTS_PER_MINUTE;
    }
}