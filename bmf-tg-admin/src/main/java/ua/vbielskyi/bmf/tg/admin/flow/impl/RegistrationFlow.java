package ua.vbielskyi.bmf.tg.admin.flow.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.vbielskyi.bmf.tg.admin.flow.FlowHandler;
import ua.vbielskyi.bmf.tg.admin.model.RegistrationData;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.UserService;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Flow handler for the user registration process
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationFlow implements FlowHandler {

    private final LocalizationService localizationService;
    private final UserSessionService sessionService;
    private final UserService userService;

    // Regex pattern for email validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    // Regex pattern for phone number validation
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    @Override
    public BotApiMethod<?> handleUpdate(Update update, UserSession session) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return invalidInputMessage(update.getMessage().getChatId(), session);
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Check for cancel command
        if (text.equals("/cancel")) {
            return handleCancel(chatId, session);
        }

        // Get or create registration data in session
        RegistrationData regData = session.getRegistrationData();
        if (regData == null) {
            regData = new RegistrationData();
            session.setRegistrationData(regData);
        }

        // Process based on current state
        switch (session.getState()) {
            case REGISTRATION_NAME:
                return handleName(chatId, text, session, regData);

            case REGISTRATION_EMAIL:
                return handleEmail(chatId, text, session, regData);

            case REGISTRATION_PHONE:
                return handlePhone(chatId, text, session, regData);

            case REGISTRATION_LANGUAGE:
                return handleLanguage(chatId, text, session, regData);

            case REGISTRATION_CONFIRMATION:
                return handleConfirmation(chatId, text, session, regData);

            default:
                // If somehow we got here with wrong state
                session.setState(UserSessionState.MAIN_MENU);
                sessionService.saveSession(session);
                return new SendMessage(chatId.toString(),
                        localizationService.getMessage("error.invalid_state", session));
        }
    }

    private SendMessage handleName(Long chatId, String text, UserSession session, RegistrationData regData) {
        // Validate name
        if (text.length() < 3 || text.length() > 100) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("registration.name.validation", session));
        }

        // Update registration data
        regData.setFullName(text);
        session.setState(UserSessionState.REGISTRATION_EMAIL);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("registration.email.prompt", session))
                .build();
    }

    private SendMessage handleEmail(Long chatId, String text, UserSession session, RegistrationData regData) {
        // Validate email
        if (!EMAIL_PATTERN.matcher(text).matches()) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("registration.email.validation", session));
        }

        // Check if email is already in use
        if (userService.isEmailInUse(text)) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("registration.email.in_use", session));
        }

        // Update registration data
        regData.setEmail(text);
        session.setState(UserSessionState.REGISTRATION_PHONE);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("registration.phone.prompt", session))
                .build();
    }

    private SendMessage handlePhone(Long chatId, String text, UserSession session, RegistrationData regData) {
        // Validate phone number
        if (!PHONE_PATTERN.matcher(text).matches()) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("registration.phone.validation", session));
        }

        // Update registration data
        regData.setPhoneNumber(text);
        session.setState(UserSessionState.REGISTRATION_LANGUAGE);
        sessionService.saveSession(session);

        // Create language selection keyboard
        InlineKeyboardMarkup inlineKeyboard = createLanguageKeyboard();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("registration.language.prompt", session))
                .replyMarkup(inlineKeyboard)
                .build();
    }

    private SendMessage handleLanguage(Long chatId, String text, UserSession session, RegistrationData regData) {
        // Validate language selection
        String language = text.toLowerCase();
        if (!isValidLanguage(language)) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("registration.language.validation", session));
        }

        // Update registration data
        regData.setPreferredLanguage(language);
        session.setState(UserSessionState.REGISTRATION_CONFIRMATION);
        sessionService.saveSession(session);

        return showConfirmation(chatId, session);
    }

    private SendMessage handleConfirmation(Long chatId, String text, UserSession session, RegistrationData regData) {
        String confirmText = localizationService.getMessage("action.confirm", session);
        String cancelText = localizationService.getMessage("action.cancel", session);

        if (text.equalsIgnoreCase(confirmText) || text.equalsIgnoreCase("yes")) {
            return completeRegistration(chatId, session);
        } else if (text.equalsIgnoreCase(cancelText) || text.equalsIgnoreCase("no")) {
            session.setState(UserSessionState.MAIN_MENU);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("registration.cancelled", session))
                    .build();
        } else {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("error.invalid_input", session) + "\n\n" +
                            localizationService.getMessage("registration.confirm_prompt", session));
        }
    }

    /**
     * Show registration details confirmation screen
     */
    public SendMessage showConfirmation(Long chatId, UserSession session) {
        RegistrationData regData = session.getRegistrationData();

        // Create message with registration details
        String message = localizationService.getMessage("registration.confirmation", session,
                regData.getFullName(),
                regData.getEmail(),
                regData.getPhoneNumber(),
                getLanguageName(regData.getPreferredLanguage(), session));

        // Create confirmation keyboard
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Confirm button
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText(localizationService.getMessage("action.confirm", session));
        confirmButton.setCallbackData("confirm:registration:yes");
        row1.add(confirmButton);

        // Cancel button
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(localizationService.getMessage("action.cancel", session));
        cancelButton.setCallbackData("confirm:registration:no");
        row1.add(cancelButton);

        keyboard.add(row1);
        inlineKeyboard.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .parseMode("Markdown")
                .replyMarkup(inlineKeyboard)
                .build();
    }

    /**
     * Complete the registration process
     */
    public SendMessage completeRegistration(Long chatId, UserSession session) {
        RegistrationData regData = session.getRegistrationData();

        try {
            // Save user to database
            userService.registerUser(
                    session.getUserId(),
                    regData.getFullName(),
                    regData.getEmail(),
                    regData.getPhoneNumber(),
                    regData.getPreferredLanguage()
            );

            // Update language preference in session
            session.setLanguage(regData.getPreferredLanguage());

            // Mark registration as complete
            regData.setConfirmed(true);

            // Reset state to main menu
            session.setState(UserSessionState.MAIN_MENU);
            sessionService.saveSession(session);

            // Return success message
            String successMessage = localizationService.getMessage("registration.completed", session,
                    regData.getFullName());

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(successMessage)
                    .parseMode("Markdown")
                    .build();

        } catch (Exception e) {
            log.error("Error completing registration", e);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("error.registration", session))
                    .build();
        }
    }

    /**
     * Handle cancel command
     */
    private SendMessage handleCancel(Long chatId, UserSession session) {
        session.setState(UserSessionState.MAIN_MENU);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("action.cancel", session))
                .build();
    }

    /**
     * Create invalid input message
     */
    private SendMessage invalidInputMessage(Long chatId, UserSession session) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_input", session))
                .build();
    }

    /**
     * Create language selection keyboard
     */
    private InlineKeyboardMarkup createLanguageKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Row for languages
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton englishButton = new InlineKeyboardButton();
        englishButton.setText("English 🇬🇧");
        englishButton.setCallbackData("language:en");
        row.add(englishButton);

        InlineKeyboardButton ukrainianButton = new InlineKeyboardButton();
        ukrainianButton.setText("Українська 🇺🇦");
        ukrainianButton.setCallbackData("language:uk");
        row.add(ukrainianButton);

        InlineKeyboardButton russianButton = new InlineKeyboardButton();
        russianButton.setText("Русский 🇷🇺");
        russianButton.setCallbackData("language:ru");
        row.add(russianButton);

        keyboard.add(row);
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    /**
     * Check if a language code is valid
     */
    private boolean isValidLanguage(String language) {
        return language.equals("en") || language.equals("uk") || language.equals("ru");
    }

    /**
     * Get localized language name
     */
    private String getLanguageName(String languageCode, UserSession session) {
        return localizationService.getMessage("language." + languageCode, session);
    }
}