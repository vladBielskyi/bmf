package ua.vbielskyi.bmf.tg.tenant.handler;

import java.util.UUID;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.core.tg.BotRegistry;
import ua.vbielskyi.bmf.core.tg.handler.BotUpdateHandler;
import ua.vbielskyi.bmf.core.tg.model.BotType;
import ua.vbielskyi.bmf.core.tg.service.impl.CachedBotRegistry;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBotHandler implements BotUpdateHandler {

    private final BotRegistry botRegistry;
    private final CachedBotRegistry cachedBotRegistry;
    private final CustomerSessionService sessionService;
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final WebAppHandler webAppHandler;

    @PostConstruct
    public void init() {
        // Register this handler with the registry
        botRegistry.registerHandler(this);
        log.info("Registered tenant bot handler");
    }

    @Override
    public BotApiMethod<?> handleUpdate(Update update, UUID tenantId) {
        if (tenantId == null) {
            log.error("Tenant ID cannot be null for tenant bot");
            return null;
        }

        // Verify tenant exists and is active
        CachedBotRegistry.BotConfig config = cachedBotRegistry.getBotConfig(BotType.TENANT, tenantId);
        if (config == null || !config.isActive()) {
            log.error("No active tenant configuration found for: {}", tenantId);
            return null;
        }

        try {
            // Set tenant context for this thread
            TenantContext.setCurrentTenant(tenantId);

            // Extract user ID
            Long userId = extractUserId(update);
            if (userId == null) {
                log.warn("Received update without user ID for tenant {}", tenantId);
                return null;
            }

            // Extract chat ID
            Long chatId = extractChatId(update);
            if (chatId == null) {
                log.warn("Received update without chat ID for tenant {}", tenantId);
                return null;
            }

            // Get or create session
            CustomerSession session = sessionService.getOrCreateSession(userId, tenantId);
            session.setChatId(chatId);

            // Update user info if available
            updateUserInfo(update, session);

            // Process the update based on type
            BotApiMethod<?> response;
            if (update.hasMessage()) {
                if (update.getMessage().hasText()) {
                    // Handle commands
                    if (update.getMessage().getText().startsWith("/")) {
                        response = commandHandler.handleCommand(update, session);
                    } else {
                        // Handle regular messages
                        response = messageHandler.handleMessage(update, session);
                    }
                } else if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {
                    // Handle media messages
                    response = messageHandler.handleMedia(update, session);
                } else if (update.getMessage().hasWebAppData()) {
                    // Handle WebApp data
                    response = webAppHandler.handleWebAppData(update, session);
                } else {
                    response = createErrorResponse(chatId);
                }
            } else if (update.hasCallbackQuery()) {
                // Handle button callbacks
                response = messageHandler.handleCallback(update, session);
            } else {
                response = createErrorResponse(chatId);
            }

            // Save session
            sessionService.saveSession(session);

            return response;
        } catch (Exception e) {
            log.error("Error processing update for tenant {}", tenantId, e);
            return createErrorResponse(extractChatId(update));
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }

    @Override
    public boolean canHandle(BotType botType, UUID tenantId) {
        return botType == BotType.TENANT && tenantId != null;
    }

    @Override
    public BotType getBotType() {
        return BotType.TENANT;
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
    private void updateUserInfo(Update update, CustomerSession session) {
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
    private SendMessage createErrorResponse(Long chatId) {
        if (chatId == null) {
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Sorry, something went wrong. Please try again later.")
                .build();
    }
}