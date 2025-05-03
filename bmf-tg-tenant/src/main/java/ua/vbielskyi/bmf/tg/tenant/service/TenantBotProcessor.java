package ua.vbielskyi.bmf.tg.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.tg.service.BotProcessorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBotProcessor implements BotProcessorService {

    public static final String PROCESSOR_TYPE = "tenant";

    private final CustomerSessionService sessionService;
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final WebAppHandler webAppHandler;
    private final BotRegistry botRegistry;

    @PostConstruct
    public void init() {
        // Register this processor with the registry
        botRegistry.registerProcessor(this);
        log.info("Registered tenant bot processor");
    }

    @Override
    public String getProcessorType() {
        return PROCESSOR_TYPE;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update, UUID tenantId) {
        if (tenantId == null) {
            log.error("Tenant ID cannot be null for tenant bot");
            return null;
        }

        try {
            // Set tenant context
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
            BotApiMethod<?> response = processUpdateByType(update, session, tenantId);

            // Save session
            sessionService.saveSession(session);

            return response;
        } catch (Exception e) {
            log.error("Error processing update for tenant {}", tenantId, e);
            return createErrorResponse(update);
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }

    @Override
    public boolean canHandle(String processorType, UUID tenantId) {
        return PROCESSOR_TYPE.equals(processorType) && tenantId != null;
    }

    /**
     * Process an update based on its type
     */
    private BotApiMethod<?> processUpdateByType(Update update, CustomerSession session, UUID tenantId) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                // Handle commands
                if (update.getMessage().getText().startsWith("/")) {
                    return commandHandler.handleCommand(tenantId, update);
                } else {
                    // Handle regular messages
                    return messageHandler.handleMessage(tenantId, update);
                }
            } else if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {
                // Handle media messages
                return messageHandler.handleMedia(tenantId, update);
            } else if (update.getMessage().hasWebAppData()) {
                // Handle WebApp data
                return webAppHandler.handleWebAppData(tenantId, update);
            }
        } else if (update.hasCallbackQuery()) {
            // Handle button callbacks
            return messageHandler.handleCallback(tenantId, update);
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