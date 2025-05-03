package ua.vbielskyi.bmf.tg.admin.command;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handler for the /language command
 * Allows users to change their interface language
 */
@RequiredArgsConstructor
public class LanguageCommandHandler implements CommandHandler {

    private final LocalizationService localizationService;
    private final UserSessionService sessionService;

    @Override
    public BotApiMethod<?> handle(Update update, UserSession session) {
        Long chatId = update.getMessage().getChatId();

        // Set state to language settings
        session.setState(UserSessionState.LANGUAGE_SETTINGS);
        sessionService.saveSession(session);

        // Create keyboard with language options
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Get current user locale
        Locale userLocale = localizationService.getUserLocale(session);

        // Add buttons for each supported language
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (String langCode : localizationService.getSupportedLanguages()) {
            // Get localized language name
            String langName = localizationService.getLanguageName(langCode, userLocale);

            // Create button
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(langName);
            button.setCallbackData("language:" + langCode);

            row1.add(button);

            // Max 2 buttons per row
            if (row1.size() == 2) {
                keyboard.add(row1);
                row1 = new ArrayList<>();
            }
        }

        // Add any remaining buttons
        if (!row1.isEmpty()) {
            keyboard.add(row1);
        }

        // Back button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(localizationService.getMessage("action.back", session));
        backButton.setCallbackData("language:back");
        backRow.add(backButton);
        keyboard.add(backRow);

        inlineKeyboard.setKeyboard(keyboard);

        // Create message
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("language.selection", session))
                .replyMarkup(inlineKeyboard)
                .build();
    }
}