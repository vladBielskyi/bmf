package ua.vbielskyi.bmf.tg.admin.handler.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.AdminSessionService;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;

@Component
@RequiredArgsConstructor
public class RegisterShopCommandHandler implements CommandHandler {

    private final AdminSessionService sessionService;
    private final LocalizationService localizationService;

    @Override
    public String getCommand() {
        return "register";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        UserSession session = sessionService.getOrCreateSession(message.getUserId());

        // Set state to shop registration - shop name
        session.setState(UserSessionState.SHOP_SETUP_NAME);
        sessionService.saveSession(session);

        // Get localized shop registration message
        String registerMessage = localizationService.getMessage("shop.register.name", message.getUserId());

        return BotResponse.text(message.getChatId(), registerMessage);
    }
}