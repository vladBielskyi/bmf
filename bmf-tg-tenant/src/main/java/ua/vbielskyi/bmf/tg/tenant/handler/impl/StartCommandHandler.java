package ua.vbielskyi.bmf.tg.tenant.handler.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.tenant.service.CustomerSessionService;

/**
 * Handler for the /start command
 */
@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final CustomerSessionService sessionService;

    @Override
    public String getCommand() {
        return "start";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        // Check if this is for admin or tenant bot
        if (message.getTenantId() == null) {
            // Admin bot start command
            return handleAdminStart(message);
        } else {
            // Tenant bot start command
            return handleTenantStart(message);
        }
    }

    private BotResponse handleAdminStart(BotMessage message) {
        return BotResponse.text(message.getChatId(),
                "Welcome to the BMF Admin Bot! This bot helps you manage your flower shop business.\n\n" +
                        "Use /register to register a new account\n" +
                        "Use /myshops to view your shops\n" +
                        "Use /newshop to create a new shop");
    }

    private BotResponse handleTenantStart(BotMessage message) {
        // Get or create customer session
        var session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());

        // Update session state to main menu
        session.setState("MAIN_MENU");
        sessionService.saveSession(session);

        return BotResponse.text(message.getChatId(),
                "Welcome to our flower shop! Browse our catalog by category or search for specific flowers.\n\n" +
                        "Use /catalog to browse our products\n" +
                        "Use /cart to view your shopping cart\n" +
                        "Use /orders to view your order history");
    }
}