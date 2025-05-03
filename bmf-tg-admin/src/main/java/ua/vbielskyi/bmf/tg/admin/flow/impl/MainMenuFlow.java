package ua.vbielskyi.bmf.tg.admin.flow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.vbielskyi.bmf.tg.admin.flow.FlowHandler;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow handler for the main menu interactions
 */
@Component
@RequiredArgsConstructor
public class MainMenuFlow implements FlowHandler {

    private final LocalizationService localizationService;
    private final UserSessionService sessionService;

    @Override
    public BotApiMethod<?> handleUpdate(Update update, UserSession session) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return null;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Handle main menu selections based on localized text
        String myShopsText = localizationService.getMessage("menu.my_shops", session);
        String newShopText = localizationService.getMessage("menu.new_shop", session);
        String settingsText = localizationService.getMessage("menu.settings", session);
        String helpText = localizationService.getMessage("menu.help", session);

        if (text.equals(myShopsText)) {
            return handleMyShops(chatId, session);
        } else if (text.equals(newShopText)) {
            return handleNewShop(chatId, session);
        } else if (text.equals(settingsText)) {
            return handleSettings(chatId, session);
        } else if (text.equals(helpText)) {
            return handleHelp(chatId, session);
        }

        // If we're in settings menu, check for settings options
        if (session.getState() == UserSessionState.SETTINGS_MENU) {
            return handleSettingsMenuOption(chatId, text, session);
        }

        // Default response for unrecognized input
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_input", session))
                .build();
    }

    /**
     * Show the main menu
     */
    public SendMessage showMainMenu(Long chatId, UserSession session) {
        session.setState(UserSessionState.MAIN_MENU);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("welcome.menu_prompt", session))
                .replyMarkup(createMainMenuKeyboard(session))
                .build();
    }

    /**
     * Show the settings menu
     */
    public SendMessage showSettingsMenu(Long chatId, UserSession session) {
        session.setState(UserSessionState.SETTINGS_MENU);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("settings.prompt", session))
                .replyMarkup(createSettingsKeyboard(session))
                .build();
    }

    /**
     * Handle the "My Shops" option
     */
    private SendMessage handleMyShops(Long chatId, UserSession session) {
        // This would typically query the database for the user's shops
        // For now, just display a placeholder message
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("shops.loading", session))
                .build();
    }

    /**
     * Handle the "New Shop" option
     */
    private SendMessage handleNewShop(Long chatId, UserSession session) {
        // Start the shop setup flow
        session.setState(UserSessionState.SHOP_SETUP_NAME);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("shop.setup.start", session) + "\n\n" +
                        localizationService.getMessage("shop.setup.name", session))
                .build();
    }

    /**
     * Handle the "Settings" option
     */
    private SendMessage handleSettings(Long chatId, UserSession session) {
        return showSettingsMenu(chatId, session);
    }

    /**
     * Handle the "Help" option
     */
    private SendMessage handleHelp(Long chatId, UserSession session) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("help.text", session))
                .parseMode("Markdown")
                .build();
    }

    /**
     * Handle settings menu options
     */
    private SendMessage handleSettingsMenuOption(Long chatId, String text, UserSession session) {
        // Get localized texts for settings options
        String profileText = localizationService.getMessage("settings.profile", session);
        String notificationsText = localizationService.getMessage("settings.notifications", session);
        String subscriptionText = localizationService.getMessage("settings.subscription", session);
        String securityText = localizationService.getMessage("settings.security", session);
        String languageText = localizationService.getMessage("settings.language", session);
        String backText = localizationService.getMessage("menu.back", session);

        if (text.equals(profileText)) {
            session.setState(UserSessionState.PROFILE_SETTINGS);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("settings.profile.info", session))
                    .build();

        } else if (text.equals(notificationsText)) {
            session.setState(UserSessionState.NOTIFICATION_SETTINGS);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("settings.notifications.info", session))
                    .build();

        } else if (text.equals(subscriptionText)) {
            session.setState(UserSessionState.SUBSCRIPTION_SETTINGS);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("settings.subscription.info", session))
                    .build();

        } else if (text.equals(securityText)) {
            session.setState(UserSessionState.SECURITY_SETTINGS);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("settings.security.info", session))
                    .build();

        } else if (text.equals(languageText)) {
            session.setState(UserSessionState.LANGUAGE_SETTINGS);
            sessionService.saveSession(session);

            // This would typically display a language selection menu
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("language.selection", session))
                    .build();

        } else if (text.equals(backText)) {
            return showMainMenu(chatId, session);
        }

        // Default response for unrecognized input
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_input", session))
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

    /**
     * Create the settings menu keyboard
     */
    private ReplyKeyboardMarkup createSettingsKeyboard(UserSession session) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1
        KeyboardRow row1 = new KeyboardRow();
        row1.add(localizationService.getMessage("settings.profile", session));
        row1.add(localizationService.getMessage("settings.notifications", session));
        keyboard.add(row1);

        // Row 2
        KeyboardRow row2 = new KeyboardRow();
        row2.add(localizationService.getMessage("settings.subscription", session));
        row2.add(localizationService.getMessage("settings.security", session));
        keyboard.add(row2);

        // Row 3
        KeyboardRow row3 = new KeyboardRow();
        row3.add(localizationService.getMessage("settings.language", session));
        keyboard.add(row3);

        // Row 4
        KeyboardRow row4 = new KeyboardRow();
        row4.add(localizationService.getMessage("menu.back", session));
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}