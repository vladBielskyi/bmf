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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LanguageCommandHandler implements CommandHandler {

    private final AdminSessionService sessionService;
    private final LocalizationService localizationService;

    @Override
    public String getCommand() {
        return "language";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        UserSession session = sessionService.getOrCreateSession(message.getUserId());

        // Set state to language selection
        session.setState(UserSessionState.LANGUAGE_SETTINGS);
        sessionService.saveSession(session);

        // Get localized language selection message
        String langMessage = localizationService.getMessage("language.select", message.getUserId());

        // Create inline keyboard for language selection
        List<List<Object>> inlineKeyboard = new ArrayList<>();

        // Row 1
        List<Object> row = new ArrayList<>();
        row.add(createInlineButton("🇺🇦 Українська", "lang_uk"));
        row.add(createInlineButton("🇬🇧 English", "lang_en"));
        inlineKeyboard.add(row);

       // return BotResponse.createWithInlineKeyboard(message.getChatId(), langMessage, inlineKeyboard);
        return null;
    }

    private Object createInlineButton(String text, String callbackData) {
        return new Object[] { text, callbackData };
    }
}