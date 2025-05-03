package ua.vbielskyi.bmf.tg.admin.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.tg.admin.flow.impl.MainMenuFlow;
import ua.vbielskyi.bmf.tg.admin.flow.ShopSetupFlow;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

import java.util.Locale;

/**
 * Handler for callback queries from inline keyboards
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final UserSessionService sessionService;
    private final LocalizationService localizationService;
    private final MainMenuFlow mainMenuFlow;
    private final ShopSetupFlow shopSetupFlow;

    /**
     * Handle a callback query
     */
    public BotApiMethod<?> handle(Update update, UserSession session) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        log.debug("Processing callback query: {}", callbackData);

        try {
            // First, acknowledge the callback to remove loading indicator
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(update.getCallbackQuery().getId());

            // Process different callback types
            if (callbackData.startsWith("language:")) {
                return handleLanguageCallback(callbackData, chatId, messageId, session);
            } else if (callbackData.startsWith("subscription:")) {
                return handleSubscriptionCallback(callbackData, chatId, messageId, session);
            } else if (callbackData.startsWith("shop:")) {
                return handleShopCallback(callbackData, chatId, messageId, session);
            } else if (callbackData.startsWith("confirm:")) {
                return handleConfirmationCallback(callbackData, chatId, messageId, session);
            } else if (callbackData.equals("back_to_main")) {
                return handleBackToMainCallback(chatId, session);
            }

            // Default response if no handler matched
            return answer;

        } catch (Exception e) {
            log.error("Error processing callback query: {}", e.getMessage(), e);

            // Create error response
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("error.general", session))
                    .build();
        }
    }

    /**
     * Handle language selection callbacks
     */
    private BotApiMethod<?> handleLanguageCallback(String callbackData, Long chatId,
                                                   Integer messageId, UserSession session) {
        String langCode = callbackData.split(":")[1];

        if (langCode.equals("back")) {
            session.setState(UserSessionState.SETTINGS_MENU);
            sessionService.saveSession(session);
            return mainMenuFlow.showSettingsMenu(chatId, session);
        }

        // Update user language
        session.setLanguage(langCode);
        sessionService.saveSession(session);

        // Get language name in the new language
        Locale newLocale = Locale.forLanguageTag(langCode);
        String languageName = localizationService.getLanguageName(langCode, newLocale);

        // Confirm language change
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(localizationService.getMessage("language.changed", session, languageName));

        return editMessage;
    }

    /**
     * Handle subscription plan selection callbacks
     */
    private BotApiMethod<?> handleSubscriptionCallback(String callbackData, Long chatId,
                                                       Integer messageId, UserSession session) {
        String plan = callbackData.split(":")[1];

        if (session.getState() == UserSessionState.SHOP_SETUP_SUBSCRIPTION && session.getShopSetupData() != null) {
            session.getShopSetupData().setSubscriptionPlan(plan);
            session.setState(UserSessionState.SHOP_SETUP_CONFIRMATION);
            sessionService.saveSession(session);

            return shopSetupFlow.showConfirmation(chatId, session);
        }

        // If not in shop setup, maybe user is upgrading an existing shop
        // Handle that case here...

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_state", session))
                .build();
    }

    /**
     * Handle shop selection callbacks
     */
    private BotApiMethod<?> handleShopCallback(String callbackData, Long chatId,
                                               Integer messageId, UserSession session) {
        String[] parts = callbackData.split(":");
        String action = parts[1];
        String shopId = parts.length > 2 ? parts[2] : null;

        // Handle different shop actions (view, edit, delete, etc.)
        switch (action) {
            case "view":
                // Set current shop and show shop management menu
                if (shopId != null) {
                    session.setCurrentShopId(Long.parseLong(shopId));
                    session.setState(UserSessionState.SHOP_MANAGEMENT);
                    sessionService.saveSession(session);

                    // TODO: Implement shop management menu
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Shop management menu for shop ID: " + shopId)
                            .build();
                }
                break;

            case "list":
                // Show list of shops
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Your shops will be listed here")
                        .build();
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_action", session))
                .build();
    }

    /**
     * Handle confirmation callbacks
     */
    private BotApiMethod<?> handleConfirmationCallback(String callbackData, Long chatId,
                                                       Integer messageId, UserSession session) {
        String[] parts = callbackData.split(":");
        String action = parts[1];
        String confirmed = parts[2];

        if (action.equals("shop_setup") && session.isInShopSetupFlow()) {
            if (confirmed.equals("yes")) {
                // Confirm shop creation
                return shopSetupFlow.createShop(chatId, session);
            } else {
                // Cancel shop creation
                session.setState(UserSessionState.MAIN_MENU);
                sessionService.saveSession(session);

                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(localizationService.getMessage("shop.setup.cancelled", session))
                        .build();
            }
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("error.invalid_action", session))
                .build();
    }

    /**
     * Handle back to main menu callback
     */
    private BotApiMethod<?> handleBackToMainCallback(Long chatId, UserSession session) {
        session.setState(UserSessionState.MAIN_MENU);
        sessionService.saveSession(session);

        return mainMenuFlow.showMainMenu(chatId, session);
    }
}