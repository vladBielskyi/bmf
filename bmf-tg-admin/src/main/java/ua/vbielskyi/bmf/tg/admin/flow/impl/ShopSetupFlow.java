package ua.vbielskyi.bmf.tg.admin.flow.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.vbielskyi.bmf.common.model.tenant.SubscriptionPlan;
import ua.vbielskyi.bmf.common.model.tenant.Tenant;
import ua.vbielskyi.bmf.tg.admin.flow.FlowHandler;
import ua.vbielskyi.bmf.tg.admin.model.ShopSetupData;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.TenantService;
import ua.vbielskyi.bmf.tg.admin.service.UserSessionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Flow handler for the shop setup process
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopSetupFlow implements FlowHandler {

    private final LocalizationService localizationService;
    private final UserSessionService sessionService;
    private final TenantService tenantService;

    // Regex pattern for Telegram bot token validation
    private static final Pattern BOT_TOKEN_PATTERN =
            Pattern.compile("^[0-9]{9}:[a-zA-Z0-9_-]{35}$");

    // Regex pattern for Telegram bot username validation
    private static final Pattern BOT_USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{5,32}$");

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

        // Get or create shop setup data in session
        ShopSetupData setupData = session.getShopSetupData();
        if (setupData == null) {
            setupData = new ShopSetupData();
            session.setShopSetupData(setupData);
        }

        // Process based on current state
        switch (session.getState()) {
            case SHOP_SETUP_NAME:
                return handleShopName(chatId, text, session, setupData);

            case SHOP_SETUP_DESCRIPTION:
                return handleShopDescription(chatId, text, session, setupData);

            case SHOP_SETUP_BOT_TOKEN:
                return handleBotToken(chatId, text, session, setupData);

            case SHOP_SETUP_BOT_USERNAME:
                return handleBotUsername(chatId, text, session, setupData);

            case SHOP_SETUP_SUBSCRIPTION:
                return handleSubscriptionPlan(chatId, text, session, setupData);

            case SHOP_SETUP_CONFIRMATION:
                return handleConfirmation(chatId, text, session, setupData);

            default:
                // If somehow we got here with wrong state
                session.setState(UserSessionState.MAIN_MENU);
                sessionService.saveSession(session);
                return new SendMessage(chatId.toString(),
                        localizationService.getMessage("error.invalid_state", session));
        }
    }

    private SendMessage handleShopName(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        // Validate shop name
        if (text.length() < 3 || text.length() > 50) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("shop.setup.name.validation", session));
        }

        // Update setup data
        setupData.setShopName(text);
        session.setState(UserSessionState.SHOP_SETUP_DESCRIPTION);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("shop.setup.description", session))
                .build();
    }

    private SendMessage handleShopDescription(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        // Update setup data
        setupData.setDescription(text);
        session.setState(UserSessionState.SHOP_SETUP_BOT_TOKEN);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("shop.setup.bot_token", session))
                .build();
    }

    private SendMessage handleBotToken(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        // Validate bot token
        if (!BOT_TOKEN_PATTERN.matcher(text).matches()) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("shop.setup.bot_token.invalid", session));
        }

        // Check if token is already in use
        if (tenantService.isBotTokenInUse(text)) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("shop.setup.bot_token.in_use", session));
        }

        // Update setup data
        setupData.setBotToken(text);
        session.setState(UserSessionState.SHOP_SETUP_BOT_USERNAME);
        sessionService.saveSession(session);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.getMessage("shop.setup.bot_username", session))
                .build();
    }

    private SendMessage handleBotUsername(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        // Validate username - must be 5-32 chars and end with 'bot'
        if (!BOT_USERNAME_PATTERN.matcher(text).matches() || !text.endsWith("bot")) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("shop.setup.bot_username.invalid", session));
        }

        // Update setup data
        setupData.setBotUsername(text);
        session.setState(UserSessionState.SHOP_SETUP_SUBSCRIPTION);
        sessionService.saveSession(session);

        // Create keyboard for subscription selection
        InlineKeyboardMarkup inlineKeyboard = createSubscriptionKeyboard(session);

        // Build message with subscription details
        StringBuilder message = new StringBuilder(
                localizationService.getMessage("shop.setup.subscription", session) + "\n\n");

        message.append("*").append(localizationService.getMessage("shop.setup.subscription.free", session)).append("*\n\n");
        message.append("*").append(localizationService.getMessage("shop.setup.subscription.basic", session)).append("*\n\n");
        message.append("*").append(localizationService.getMessage("shop.setup.subscription.premium", session)).append("*\n\n");
        message.append("*").append(localizationService.getMessage("shop.setup.subscription.enterprise", session)).append("*");

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(message.toString())
                .parseMode("Markdown")
                .replyMarkup(inlineKeyboard)
                .build();
    }

    private SendMessage handleSubscriptionPlan(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        // Validate subscription plan
        try {
            SubscriptionPlan plan = SubscriptionPlan.valueOf(text.toUpperCase());
            setupData.setSubscriptionPlan(plan.name());
            session.setState(UserSessionState.SHOP_SETUP_CONFIRMATION);
            sessionService.saveSession(session);

            return showConfirmation(chatId, session);
        } catch (IllegalArgumentException e) {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("shop.setup.subscription.invalid", session));
        }
    }

    private SendMessage handleConfirmation(Long chatId, String text, UserSession session, ShopSetupData setupData) {
        String confirmText = localizationService.getMessage("action.confirm", session);
        String cancelText = localizationService.getMessage("action.cancel", session);

        if (text.equalsIgnoreCase(confirmText) || text.equalsIgnoreCase("yes")) {
            return createShop(chatId, session);
        } else if (text.equalsIgnoreCase(cancelText) || text.equalsIgnoreCase("no")) {
            session.setState(UserSessionState.MAIN_MENU);
            sessionService.saveSession(session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("shop.setup.cancelled", session))
                    .build();
        } else {
            return new SendMessage(chatId.toString(),
                    localizationService.getMessage("error.invalid_input", session) + "\n\n" +
                            localizationService.getMessage("shop.setup.confirm_prompt", session));
        }
    }

    /**
     * Show shop details confirmation screen
     */
    public SendMessage showConfirmation(Long chatId, UserSession session) {
        ShopSetupData setupData = session.getShopSetupData();

        // Create message with shop details
        String message = localizationService.getMessage("shop.setup.confirmation", session,
                setupData.getShopName(),
                setupData.getDescription(),
                setupData.getBotUsername(),
                setupData.getSubscriptionPlan());

        // Create confirmation keyboard
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Confirm button
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText(localizationService.getMessage("action.confirm", session));
        confirmButton.setCallbackData("confirm:shop_setup:yes");
        row1.add(confirmButton);

        // Cancel button
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(localizationService.getMessage("action.cancel", session));
        cancelButton.setCallbackData("confirm:shop_setup:no");
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
     * Create a new shop based on the setup data
     */
    public SendMessage createShop(Long chatId, UserSession session) {
        ShopSetupData setupData = session.getShopSetupData();

        try {
            // Create tenant object
            Tenant tenant = Tenant.builder()
                    .id(UUID.randomUUID())
                    .name(session.getFirstName() + " " + session.getLastName())
                    .shopName(setupData.getShopName())
                    .description(setupData.getDescription())
                    .telegramBotToken(setupData.getBotToken())
                    .telegramBotUsername(setupData.getBotUsername())
                    .subscriptionPlan(SubscriptionPlan.valueOf(setupData.getSubscriptionPlan()))
                    .subscriptionExpiryDate(LocalDateTime.now().plusMonths(1))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Save tenant
            tenantService.saveTenant(tenant, session.getUserId());

            // Reset state to main menu
            session.setState(UserSessionState.MAIN_MENU);
            session.setShopSetupData(null);
            sessionService.saveSession(session);

            // Return success message
            String successMessage = localizationService.getMessage("shop.setup.completed", session,
                    setupData.getShopName(), setupData.getBotUsername()) + "\n\n" +
                    localizationService.getMessage("shop.setup.next_steps", session);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(successMessage)
                    .parseMode("Markdown")
                    .build();

        } catch (Exception e) {
            log.error("Error creating shop", e);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(localizationService.getMessage("error.shop_creation", session))
                    .build();
        }
    }

    /**
     * Handle cancel command
     */
    private SendMessage handleCancel(Long chatId, UserSession session) {
        session.setState(UserSessionState.MAIN_MENU);
        session.setShopSetupData(null);
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
     * Create subscription plan selection keyboard
     */
    private InlineKeyboardMarkup createSubscriptionKeyboard(UserSession session) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Row 1: FREE and BASIC
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton freeButton = new InlineKeyboardButton();
        freeButton.setText("FREE");
        freeButton.setCallbackData("subscription:FREE");
        row1.add(freeButton);

        InlineKeyboardButton basicButton = new InlineKeyboardButton();
        basicButton.setText("BASIC");
        basicButton.setCallbackData("subscription:BASIC");
        row1.add(basicButton);

        keyboard.add(row1);

        // Row 2: PREMIUM and ENTERPRISE
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton premiumButton = new InlineKeyboardButton();
        premiumButton.setText("PREMIUM");
        premiumButton.setCallbackData("subscription:PREMIUM");
        row2.add(premiumButton);

        InlineKeyboardButton enterpriseButton = new InlineKeyboardButton();
        enterpriseButton.setText("ENTERPRISE");
        enterpriseButton.setCallbackData("subscription:ENTERPRISE");
        row2.add(enterpriseButton);

        keyboard.add(row2);

        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }
}