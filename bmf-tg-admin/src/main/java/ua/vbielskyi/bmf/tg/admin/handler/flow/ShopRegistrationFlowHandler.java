package ua.vbielskyi.bmf.tg.admin.handler.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;
import ua.vbielskyi.bmf.tg.admin.service.AdminSessionService;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.TenantManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopRegistrationFlowHandler {

    private final AdminSessionService sessionService;
    private final LocalizationService localizationService;
    private final TenantManagementService tenantService;

    private static final Pattern BOT_TOKEN_PATTERN = Pattern.compile("^[0-9]{9}:[a-zA-Z0-9_-]{35}$");
    private static final Pattern BOT_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{5,32}$");

    public BotResponse handleMessage(BotMessage message) {
        UserSession session = sessionService.getOrCreateSession(message.getUserId());
        String text = message.getText();

        if (text != null && text.equals("/cancel")) {
            // Reset session to main menu
            session.setState(UserSessionState.MAIN_MENU);
            sessionService.saveSession(session);

            return BotResponse.text(message.getChatId(),
                    localizationService.getMessage("shop.register.canceled", message.getUserId()));
        }

        switch (session.getState()) {
            case SHOP_SETUP_NAME:
                return handleShopName(message, session, text);

            case SHOP_SETUP_DESCRIPTION:
                return handleShopDescription(message, session, text);

            case SHOP_SETUP_BOT_TOKEN:
                return handleBotToken(message, session, text);

            case SHOP_SETUP_BOT_USERNAME:
                return handleBotUsername(message, session, text);

            case SHOP_SETUP_CONFIRMATION:
                return handleConfirmation(message, session, text);

            default:
                return BotResponse.text(message.getChatId(),
                        localizationService.getMessage("error.unknown_state", message.getUserId()));
        }
    }

    private BotResponse handleShopName(BotMessage message, UserSession session, String text) {
        // Store shop name
        session.setAttribute("shopName", text);

        // Move to next step
        session.setState(UserSessionState.SHOP_SETUP_DESCRIPTION);
        sessionService.saveSession(session);

        return BotResponse.text(message.getChatId(),
                localizationService.getMessage("shop.register.description", message.getUserId()));
    }

    private BotResponse handleShopDescription(BotMessage message, UserSession session, String text) {
        // Store shop description
        session.setAttribute("shopDescription", text);

        // Move to next step
        session.setState(UserSessionState.SHOP_SETUP_BOT_TOKEN);
        sessionService.saveSession(session);

        // Provide more detailed instructions for bot token
        String instructionMessage = localizationService.getMessage("shop.register.bot_token", message.getUserId()) +
                "\n\n" + localizationService.getMessage("shop.register.token_instructions", message.getUserId());

        return BotResponse.text(message.getChatId(), instructionMessage);
    }

    private BotResponse handleBotToken(BotMessage message, UserSession session, String text) {
        // Validate bot token format
        if (!BOT_TOKEN_PATTERN.matcher(text).matches()) {
            return BotResponse.text(message.getChatId(),
                    localizationService.getMessage("shop.register.invalid_token", message.getUserId()));
        }

        // Check if token already in use
//        if (tenantService.isBotTokenInUse(text)) {
//            return BotResponse.text(message.getChatId(),
//                    localizationService.getMessage("shop.register.token_in_use", message.getUserId()));
//        }

        // Store bot token
        session.setAttribute("botToken", text);

        // Move to next step
        session.setState(UserSessionState.SHOP_SETUP_BOT_USERNAME);
        sessionService.saveSession(session);

        return BotResponse.text(message.getChatId(),
                localizationService.getMessage("shop.register.bot_username", message.getUserId()));
    }

    private BotResponse handleBotUsername(BotMessage message, UserSession session, String text) {
        // Clean up input - remove @ if present
        String username = text.startsWith("@") ? text.substring(1) : text;

        // Validate bot username format
        if (!BOT_USERNAME_PATTERN.matcher(username).matches()) {
            return BotResponse.text(message.getChatId(),
                    localizationService.getMessage("shop.register.invalid_username", message.getUserId()));
        }

        // Check if username already in use
//        if (tenantService.isBotUsernameInUse(username)) {
//            return BotResponse.text(message.getChatId(),
//                    localizationService.getMessage("shop.register.username_in_use", message.getUserId()));
//        }

        // Store bot username
        session.setAttribute("botUsername", username);

        // Move to confirmation step
        session.setState(UserSessionState.SHOP_SETUP_CONFIRMATION);
        sessionService.saveSession(session);

        // Build confirmation message
        String shopName = (String) session.getAttribute("shopName");
        String shopDescription = (String) session.getAttribute("shopDescription");

        StringBuilder confirmMessage = new StringBuilder();
        confirmMessage.append(localizationService.getMessage("shop.register.confirmation", message.getUserId())).append("\n\n");
        confirmMessage.append("🌸 ").append(shopName).append("\n");
        confirmMessage.append("📝 ").append(shopDescription).append("\n");
        confirmMessage.append("🤖 @").append(username).append("\n\n");
        confirmMessage.append(localizationService.getMessage("shop.register.confirm_prompt", message.getUserId()));

        return BotResponse.text(message.getChatId(), confirmMessage.toString());
    }

    private BotResponse handleConfirmation(BotMessage message, UserSession session, String text) {
        if (text.equalsIgnoreCase(localizationService.getMessage("confirm.yes", message.getUserId())) ||
                text.equalsIgnoreCase("yes") ||
                text.equalsIgnoreCase("да") ||
                text.equalsIgnoreCase("так")) {

            // Register the shop
            try {
                String shopName = (String) session.getAttribute("shopName");
                String shopDescription = (String) session.getAttribute("shopDescription");
                String botToken = (String) session.getAttribute("botToken");
                String botUsername = (String) session.getAttribute("botUsername");
                String webhookBaseUrl = "https://botanicalmarketingflow.com/api/webhook"; // Config value

                // Create the tenant
                TenantEntity tenant = tenantService.registerShop(
                        message.getUserId(),
                        shopName, // Owner name is same as shop name initially
                        shopName,
                        shopDescription,
                        botToken,
                        botUsername,
                        webhookBaseUrl
                );

                // Reset session to main menu
                session.setState(UserSessionState.MAIN_MENU);
                sessionService.saveSession(session);

                // Success message with next steps
                StringBuilder successMessage = new StringBuilder();
                successMessage.append("✅ ").append(localizationService.getMessage("shop.register.success", message.getUserId())).append("\n\n");
                successMessage.append(localizationService.getMessage("shop.register.next_steps", message.getUserId())).append("\n\n");
                successMessage.append("1. ").append(localizationService.getMessage("shop.register.step1", message.getUserId())).append("\n");
                successMessage.append("2. ").append(localizationService.getMessage("shop.register.step2", message.getUserId())).append("\n");
                successMessage.append("3. ").append(localizationService.getMessage("shop.register.step3", message.getUserId())).append("\n\n");

                // Create inline keyboard with admin panel link
                List<List<Object>> inlineKeyboard = new ArrayList<>();
                List<Object> row = new ArrayList<>();
                row.add(createWebButton(
                        "🖥 " + localizationService.getMessage("button.web_admin", message.getUserId()),
                        "https://botanicalmarketingflow.com/admin?tenant=" + tenant.getId()
                ));
                inlineKeyboard.add(row);

                return null;
               // return BotResponse.createWithInlineKeyboard(message.getChatId(), successMessage.toString(), inlineKeyboard);

            } catch (Exception e) {
                log.error("Error registering shop", e);
                session.setState(UserSessionState.MAIN_MENU);
                sessionService.saveSession(session);

                return BotResponse.text(message.getChatId(),
                        localizationService.getMessage("shop.register.error", message.getUserId()));
            }
        } else {
            // Reset session to main menu
            session.setState(UserSessionState.MAIN_MENU);
            sessionService.saveSession(session);

            return BotResponse.text(message.getChatId(),
                    localizationService.getMessage("shop.register.canceled", message.getUserId()));
        }
    }

    private Object createWebButton(String text, String url) {
        return new Object[] { text, url };
    }
}