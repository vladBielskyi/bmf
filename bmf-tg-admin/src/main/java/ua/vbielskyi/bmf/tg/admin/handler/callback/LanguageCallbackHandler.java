package ua.vbielskyi.bmf.tg.admin.handler.callback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.telegram.handler.CallbackQueryHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.AdminSessionService;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;

@Component
@RequiredArgsConstructor
public class LanguageCallbackHandler implements CallbackQueryHandler {

    private final AdminSessionService sessionService;
    private final LocalizationService localizationService;

    @Override
    public String getCallbackPrefix() {
        return "lang_";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        String callbackData = message.getCallbackData();
        String languageCode = callbackData.substring(5); // Remove "lang_" prefix

        UserSession session = sessionService.getOrCreateSession(message.getUserId());

        // Update user language preference
        session.setLanguage(languageCode);
        session.setState(UserSessionState.MAIN_MENU);
        sessionService.saveSession(session);

        // Update language in service
        localizationService.setUserLanguage(message.getUserId(), languageCode);

        // Get localized confirmation message
        String confirmMessage = localizationService.getMessage("language.changed", message.getUserId());

        // Send confirmation and return to main menu
        BotResponse response = BotResponse.text(message.getChatId(), confirmMessage);

        // Add main menu command
       // response.addMethod(BotResponse.createCommand(message.getChatId(), "/start"));

        return response;
    }
}