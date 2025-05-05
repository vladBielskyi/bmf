package ua.vbielskyi.bmf.tg.admin.handler;

import java.util.UUID;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.tg.BotRegistry;
import ua.vbielskyi.bmf.core.tg.handler.BotUpdateHandler;
import ua.vbielskyi.bmf.core.tg.model.BotType;
import ua.vbielskyi.bmf.tg.admin.command.CommandHandler;
import ua.vbielskyi.bmf.tg.admin.command.CallbackQueryHandler;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBotHandler implements BotUpdateHandler {

    private final BotRegistry botRegistry;
    private final UserSessionService sessionService;
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    @PostConstruct
    public void init() {
        botRegistry.registerHandler(this);
        log.info("Registered admin bot handler");
    }

    @Override
    public BotApiMethod<?> handleUpdate(Update update, UUID tenantId) {
        try {
            // Extract user ID
            Long userId = extractUserId(update);
            if (userId == null) {
                log.warn("Received update without user ID");
                return null;
            }

            // Extract chat ID
            Long chatId = extractChatId(update);
            if (chatId == null) {
                log.warn("Received update without chat ID");
                return null;
            }

            // Get or create session
            UserSession session = sessionService.getOrCreateSession(userId);

            // Update user info if available
            updateUserInfo(update, session);

            // Process the update based on type
            BotApiMethod<?> response;
            if (update.hasMessage()) {
                if (update.getMessage().hasText()) {
                    // Handle commands
                    if (update.getMessage().getText().startsWith("/")) {
                        response = commandHandler.handle(update, session);
                    } else {
                        // Handle regular messages
                        response = messageHandler.handle(update, session);
                    }
                } else {
                    // Handle other types
                    response = createErrorResponse(update);
                }
            } else if (update.hasCallbackQuery()) {
                // Handle button callbacks
                response = callbackQueryHandler.handle(update, session);
            } else {
                response = createErrorResponse(update);
            }

            // Save session
            sessionService.saveSession(session);

            return response;
        } catch (Exception e) {
            log.error("Error processing admin bot update", e);
            return createErrorResponse(update);
        }
    }

    @Override
    public boolean canHandle(BotType botType, UUID tenantId) {
        return botType == BotType.ADMIN && tenantId == null;
    }

    @Override
    public BotType getBotType() {
        return BotType.ADMIN;
    }

    /**
     * Extract user ID from an update
     */
    private Long extractUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }
        return null;
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
     * Update user info in the session
     */
    private void updateUserInfo(Update update, UserSession session) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            session.setUsername(update.getMessage().getFrom().getUserName());
            session.setFirstName(update.getMessage().getFrom().getFirstName());
            session.setLastName(update.getMessage().getFrom().getLastName());
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            session.setUsername(update.getCallbackQuery().getFrom().getUserName());
            session.setFirstName(update.getCallbackQuery().getFrom().getFirstName());
            session.setLastName(update.getCallbackQuery().getFrom().getLastName());
        }
    }

    /**
     * Create an error response
     */
    private SendMessage createErrorResponse(Update update) {
        Long chatId = extractChatId(update);
        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Sorry, something went wrong. Please try again later.")
                .build();
    }
}