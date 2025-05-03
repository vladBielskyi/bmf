package ua.vbielskyi.bmf.tg.admin.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.vbielskyi.bmf.tg.admin.command.CommandDispatcher;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

/**
 * Main admin bot for BMF platform
 * Handles shop registration and platform administration
 */
@Slf4j
@Component
public class BmfAdminBot extends TelegramWebhookBot {

    private final String botUsername;
    private final String botPath;
    private final CommandDispatcher commandDispatcher;
    private final UserSessionService sessionService;

    public BmfAdminBot(
            @Value("${bot.admin.token}") String botToken,
            @Value("${bot.admin.username}") String botUsername,
            @Value("${bot.admin.webhook-path}") String botPath,
            CommandDispatcher commandDispatcher,
            UserSessionService sessionService) {
        super(botToken);
        this.botUsername = botUsername;
        this.botPath = botPath;
        this.commandDispatcher = commandDispatcher;
        this.sessionService = sessionService;

        log.info("Admin bot initialized with username: {}", botUsername);
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        try {
            // Extract user ID from update
            Long userId = extractUserId(update);
            if (userId == null) {
                log.warn("Received update without user ID");
                return null;
            }

            // Get or create user session
            var session = sessionService.getOrCreateSession(userId);

            // Dispatch to appropriate handler based on update content and user's state
            return commandDispatcher.dispatch(update, session);

        } catch (Exception e) {
            log.error("Error processing update", e);
            return createErrorResponse(update);
        }
    }

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

    private SendMessage createErrorResponse(Update update) {
        Long chatId = null;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }

        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Sorry, something went wrong. Please try again later.")
                .build();
    }

    // Helper method to send messages directly (for async operations)
    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotPath() {
        return botPath;
    }
}