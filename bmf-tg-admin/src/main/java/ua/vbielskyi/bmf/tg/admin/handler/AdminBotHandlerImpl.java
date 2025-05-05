package ua.vbielskyi.bmf.tg.admin.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.telegram.BotRegistry;
import ua.vbielskyi.bmf.core.telegram.handler.CallbackQueryHandler;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.handler.WebAppDataHandler;
import ua.vbielskyi.bmf.core.telegram.handler.impl.AbstractBotHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.tg.admin.service.AdminSessionService;

import java.util.List;
import java.util.UUID;

/**
 * Handler for the admin bot
 */
@Slf4j
@Service
public class AdminBotHandlerImpl extends AbstractBotHandler {

    private final BotRegistry botRegistry;
    private final AdminSessionService sessionService;

    @Autowired
    public AdminBotHandlerImpl(BotRegistry botRegistry,
                               AdminSessionService sessionService,
                               List<CommandHandler> commandHandlers,
                               List<CallbackQueryHandler> callbackHandlers,
                               List<WebAppDataHandler> webAppHandlers) {
        super(commandHandlers, callbackHandlers, webAppHandlers);
        this.botRegistry = botRegistry;
        this.sessionService = sessionService;
    }

    @PostConstruct
    public void init() {
        botRegistry.registerHandler(this);
        log.info("Registered admin bot handler");
    }

    @Override
    public boolean canHandle(BotType botType, UUID tenantId) {
        return botType == BotType.ADMIN && tenantId == null;
    }

    @Override
    public BotType getBotType() {
        return BotType.ADMIN;
    }

    @Override
    protected BotResponse handleText(BotMessage message) {
        // Get or create session
        var session = sessionService.getOrCreateSession(message.getUserId());

        // Handle a message based on current session state
        return switch (session.getState()) {
            case REGISTRATION_NAME, REGISTRATION_EMAIL, REGISTRATION_PHONE ->
                // These states should be handled by a flow handler
                    BotResponse.text(message.getChatId(),
                            "Registration flow is active. Please continue or use /cancel to abort.");
            case SHOP_SETUP_NAME, SHOP_SETUP_DESCRIPTION, SHOP_SETUP_BOT_TOKEN ->
                // These states should be handled by a flow handler
                    BotResponse.text(message.getChatId(),
                            "Shop setup flow is active. Please continue or use /cancel to abort.");
            default ->
                // Default text handling for main menu state
                    BotResponse.text(message.getChatId(),
                            "Use commands or buttons to interact with the admin bot.");
        };
    }

    @Override
    protected BotResponse handleOtherMessageTypes(BotMessage message) {
        // Handle other message types
        return BotResponse.text(message.getChatId(),
                "This message type is not supported by the admin bot.");
    }

    @Override
    protected boolean isAuthenticated(BotMessage message) {
        // Check if user is authenticated
        return true;
    }
}