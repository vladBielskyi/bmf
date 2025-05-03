package ua.vbielskyi.bmf.tg.admin.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.tg.service.BotProcessorService;
import ua.vbielskyi.bmf.tg.admin.handler.AdminCommandHandler;
import ua.vbielskyi.bmf.tg.admin.handler.AdminMessageHandler;

/**
 * Implementation of BotProcessorService for admin bot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBotProcessor implements BotProcessorService {

    public static final String PROCESSOR_TYPE = "admin";

    @Value("${bot.admin.token}")
    private String adminBotToken;

    private final AdminSessionService sessionService;
    private final AdminCommandHandler commandHandler;
    private final AdminMessageHandler messageHandler;
    private final AdminCallbackHandler callbackHandler;
    private final ua.vbielskyi.bmf.core.service.bot.BotRegistry botRegistry;

    @PostConstruct
    public void init() {
        // Register this processor with the registry
        botRegistry.registerProcessor(this);
        log.info("Registered admin bot processor");
    }

    @Override
    public String getProcessorType() {
        return PROCESSOR_TYPE;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update, UUID tenantId) {
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
            AdminSession session = sessionService.getOrCreateSession(userId);
            session.setChatId(chatId);

            // Update user info if available
            updateUserInfo(update, session);

            // Process the update based on type
            BotApiMethod<?> response = processUpdateByType(update, session);

            // Save session
            sessionService.saveSession(session);

            return response;

        } catch (Exception e) {
            log.error("Error processing admin bot update", e);
            return createErrorResponse(update);
        }
    }

    @Override
    public boolean canHandle(String processorType, UUID tenantId) {
        return PROCESSOR_TYPE.equals(processorType) && tenantId == null;
    }

    /**
     * Process an update based on its type
     */
    private BotApiMethod<?> processUpdateByType(Update update, AdminSession session) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                // Handle commands
                if (update.getMessage().getText().startsWith("/")) {
                    return commandHandler.handleCommand(update, session);
                } else {
                    // Handle regular messages
                    return messageHandler.handleMessage(update, session);
                }
            }
        } else if (update.hasCallbackQuery()) {
            // Handle button callbacks
            return callbackHandler.handleCallback(update, session);
        }

        return createErrorResponse(update);
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
    private void updateUserInfo(Update update, AdminSession session) {
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
