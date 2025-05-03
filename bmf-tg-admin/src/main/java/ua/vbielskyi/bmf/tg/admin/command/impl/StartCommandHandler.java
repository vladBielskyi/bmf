package ua.vbielskyi.bmf.tg.admin.command;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.vbielskyi.bmf.tg.admin.flow.impl.MainMenuFlow;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for the /start command
 */
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final MainMenuFlow mainMenuFlow;
    private final LocalizationService localizationService;

    @Override
    public BotApiMethod<?> handle(Update update, UserSession session) {
        Long chatId = update.getMessage().getChatId();

        // Update user information if available
        if (update.getMessage().getFrom() != null) {
            session.setUsername(update.getMessage().getFrom().getUserName());
            session.setFirstName(update.getMessage().getFrom().getFirstName());
            session.setLastName(update.getMessage().getFrom().getLastName());
        }

        // Set or reset to main menu state
        session.setState(UserSessionState.MAIN_MENU);

        // Prepare appropriate welcome message
        String messageKey = session.isNewUser() ? "welcome" : "welcome.returning";
        String welcomeText = localizationService.getMessage(messageKey, session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(welcomeText + "\n\n" +
                        localizationService.getMessage("welcome.menu_prompt", session))
                .replyMarkup(createMainMenuKeyboard(session))
                .build();
    }

    /**
     * Create the main menu keyboard
     */
    private ReplyKeyboardMarkup createMainMenuKeyboard(UserSession session) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1
        KeyboardRow row1 = new KeyboardRow();
        row1.add(localizationService.getMessage("menu.my_shops", session));
        row1.add(localizationService.getMessage("menu.new_shop", session));
        keyboard.add(row1);

        // Row 2
        KeyboardRow row2 = new KeyboardRow();
        row2.add(localizationService.getMessage("menu.settings", session));
        row2.add(localizationService.getMessage("menu.help", session));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}