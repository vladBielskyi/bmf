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
import ua.vbielskyi.bmf.tg.admin.service.TenantManagementService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final AdminSessionService sessionService;
    private final LocalizationService localizationService;
    private final TenantManagementService tenantService;

    @Override
    public String getCommand() {
        return "start";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        UserSession session = sessionService.getOrCreateSession(message.getUserId());
        String language = session.getLanguage() != null ? session.getLanguage() : "uk"; // Default to Ukrainian

        // Reset session to main menu
        session.setState(UserSessionState.MAIN_MENU);
        sessionService.saveSession(session);

        // Get localized welcome message
        String welcomeMessage = localizationService.getMessage("start.welcome", message.getUserId());

        // Create keyboard for main menu
        List<List<String>> keyboard = new ArrayList<>();

        // Row 1
        List<String> row1 = new ArrayList<>();
        row1.add(localizationService.getMessage("button.register_shop", message.getUserId()));
        row1.add(localizationService.getMessage("button.my_shops", message.getUserId()));
        keyboard.add(row1);

        // Row 2
        List<String> row2 = new ArrayList<>();
        row2.add(localizationService.getMessage("button.subscription_plans", message.getUserId()));
        row2.add(localizationService.getMessage("button.help", message.getUserId()));
        keyboard.add(row2);

        // Row 3
        List<String> row3 = new ArrayList<>();
        row3.add(localizationService.getMessage("button.language", message.getUserId()));
        keyboard.add(row3);

        return null;
       // return BotResponse.createWithReplyKeyboard(message.getChatId(), welcomeMessage, keyboard);
    }
}