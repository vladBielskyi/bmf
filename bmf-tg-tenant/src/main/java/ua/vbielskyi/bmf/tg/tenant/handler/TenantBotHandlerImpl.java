package ua.vbielskyi.bmf.tg.tenant.handler;

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
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;
import ua.vbielskyi.bmf.tg.tenant.service.CustomerSessionService;

import java.util.List;
import java.util.UUID;

/**
 * Handler for tenant bots
 */
@Slf4j
@Service
public class TenantBotHandlerImpl extends AbstractBotHandler {

    private final BotRegistry botRegistry;
    private final CachedBotRegistry cachedBotRegistry;
    private final CustomerSessionService sessionService;

    @Autowired
    public TenantBotHandlerImpl(BotRegistry botRegistry,
                                CachedBotRegistry cachedBotRegistry,
                                CustomerSessionService sessionService,
                                List<CommandHandler> commandHandlers,
                                List<CallbackQueryHandler> callbackHandlers,
                                List<WebAppDataHandler> webAppHandlers) {
        super(commandHandlers, callbackHandlers, webAppHandlers);
        this.botRegistry = botRegistry;
        this.cachedBotRegistry = cachedBotRegistry;
        this.sessionService = sessionService;
    }

    @PostConstruct
    public void init() {
        botRegistry.registerHandler(this);
        log.info("Registered tenant bot handler");
    }

    @Override
    public boolean canHandle(BotType botType, UUID tenantId) {
        return botType == BotType.TENANT && tenantId != null;
    }

    @Override
    public BotType getBotType() {
        return BotType.TENANT;
    }

    @Override
    protected BotResponse handleText(BotMessage message) {
        // Verify tenant exists and is active
        CachedBotRegistry.BotConfig config = cachedBotRegistry.getBotConfig(BotType.TENANT, message.getTenantId());
        if (config == null || !config.isActive()) {
            log.error("No active tenant configuration found for: {}", message.getTenantId());
            return BotResponse.text(message.getChatId(), "This bot is currently inactive.");
        }

        // Get or create session
        var session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());

        // Handle message based on current session state
        switch (session.getState()) {
            case CATALOG_BROWSING:
                // Handle catalog browsing
                return BotResponse.text(message.getChatId(),
                        "You are browsing our catalog. Use commands to navigate or type a search query.");

            case ORDER_CREATION:
                // Handle order creation flow
                return BotResponse.text(message.getChatId(),
                        "You are creating an order. Please follow the instructions or use /cancel to abort.");

            case CHECKOUT:
                // Handle checkout flow
                return BotResponse.text(message.getChatId(),
                        "You are in the checkout process. Please follow the instructions or use /cancel to abort.");

            case MAIN_MENU:
            default:
                // Default text handling for main menu state
                return BotResponse.text(message.getChatId(),
                        "Welcome to our flower shop! Use commands or buttons to browse our catalog.");
        }
    }

    @Override
    protected BotResponse handleOtherMessageTypes(BotMessage message) {
        // Handle other message types based on the type
        switch (message.getType()) {
            case LOCATION:
                return BotResponse.text(message.getChatId(),
                        "Thank you for sharing your location. We'll use it to show nearby flower shops.");

            case PHOTO:
                return BotResponse.text(message.getChatId(),
                        "Thank you for the photo. Our team will review it.");

            case CONTACT:
                return BotResponse.text(message.getChatId(),
                        "Thank you for sharing your contact information.");

            default:
                return BotResponse.text(message.getChatId(),
                        "This message type is not supported by our flower shop bot.");
        }
    }

    @Override
    protected boolean isAuthenticated(BotMessage message) {
        // For tenant bots, all users are considered authenticated
        return true;
    }
}