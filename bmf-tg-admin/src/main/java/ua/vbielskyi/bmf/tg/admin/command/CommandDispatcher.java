package ua.vbielskyi.bmf.tg.admin.command;

import jakarta.websocket.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.tg.admin.flow.FlowHandler;
import ua.vbielskyi.bmf.tg.admin.flow.impl.MainMenuFlow;
import ua.vbielskyi.bmf.tg.admin.flow.impl.RegistrationFlow;
import ua.vbielskyi.bmf.tg.admin.flow.impl.ShopSetupFlow;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;

import java.util.HashMap;
import java.util.Map;

/**
 * Central dispatcher that routes updates to appropriate handlers
 * based on command type and user state
 */
@Slf4j
@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();
    private final Map<UserSessionState, FlowHandler> flowHandlers = new HashMap<>();
    private final MessageHandler messageHandler;
    private final ua.vbielskyi.bmf.tg.admin.command.CallbackQueryHandler callbackQueryHandler;

    public CommandDispatcher(
            @Lazy MainMenuFlow mainMenuFlow,
            @Lazy RegistrationFlow registrationFlow,
            @Lazy ShopSetupFlow shopSetupFlow,
            MessageHandler messageHandler,
            ua.vbielskyi.bmf.tg.admin.command.CallbackQueryHandler callbackQueryHandler) {

        this.messageHandler = messageHandler;
        this.callbackQueryHandler = callbackQueryHandler;

        // Register command handlers
        registerCommandHandler("/start", new ua.vbielskyi.bmf.tg.admin.command.StartCommandHandler(mainMenuFlow));
        registerCommandHandler("/register", new RegisterCommandHandler(registrationFlow));
        registerCommandHandler("/myshops", new MyShopsCommandHandler());
        registerCommandHandler("/newshop", new NewShopCommandHandler(shopSetupFlow));
        registerCommandHandler("/settings", new SettingsCommandHandler());
        registerCommandHandler("/help", new HelpCommandHandler());
        registerCommandHandler("/cancel", new CancelCommandHandler(mainMenuFlow));
        registerCommandHandler("/language", new LanguageCommandHandler());

        // Register flow handlers by state
        registerFlowHandler(UserSessionState.REGISTRATION_NAME, registrationFlow);
        registerFlowHandler(UserSessionState.REGISTRATION_EMAIL, registrationFlow);
        registerFlowHandler(UserSessionState.REGISTRATION_PHONE, registrationFlow);
        registerFlowHandler(UserSessionState.REGISTRATION_LANGUAGE, registrationFlow);
        registerFlowHandler(UserSessionState.REGISTRATION_CONFIRMATION, registrationFlow);

        registerFlowHandler(UserSessionState.SHOP_SETUP_NAME, shopSetupFlow);
        registerFlowHandler(UserSessionState.SHOP_SETUP_DESCRIPTION, shopSetupFlow);
        registerFlowHandler(UserSessionState.SHOP_SETUP_BOT_TOKEN, shopSetupFlow);
        registerFlowHandler(UserSessionState.SHOP_SETUP_BOT_USERNAME, shopSetupFlow);
        registerFlowHandler(UserSessionState.SHOP_SETUP_SUBSCRIPTION, shopSetupFlow);
        registerFlowHandler(UserSessionState.SHOP_SETUP_CONFIRMATION, shopSetupFlow);

        registerFlowHandler(UserSessionState.MAIN_MENU, mainMenuFlow);
        registerFlowHandler(UserSessionState.SETTINGS_MENU, mainMenuFlow);
    }

    public BotApiMethod<?> dispatch(Update update, UserSession session) {
        // Handle commands
        if (isCommand(update)) {
            String command = update.getMessage().getText();
            CommandHandler handler = commandHandlers.get(command);

            if (handler != null) {
                log.debug("Dispatching command '{}' to {}", command, handler.getClass().getSimpleName());
                return handler.handle(update, session);
            }
        }

        // Handle callback queries
        if (update.hasCallbackQuery()) {
            log.debug("Dispatching callback query to CallbackQueryHandler");
            return callbackQueryHandler.handle(update, session);
        }

        // Handle flow-specific processing based on user state
        if (session.getState() != null && flowHandlers.containsKey(session.getState())) {
            FlowHandler handler = flowHandlers.get(session.getState());
            log.debug("Dispatching to flow handler {} for state {}",
                    handler.getClass().getSimpleName(), session.getState());
            return handler.handleUpdate(update, session);
        }

        // Handle regular messages
        if (update.hasMessage() && update.getMessage().hasText()) {
            log.debug("Dispatching text message to MessageHandler");
            return messageHandler.handle(update, session);
        }

        log.warn("No handler found for update");
        return null;
    }

    private boolean isCommand(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().startsWith("/");
    }

    private void registerCommandHandler(String command, CommandHandler handler) {
        commandHandlers.put(command, handler);
    }

    private void registerFlowHandler(UserSessionState state, FlowHandler handler) {
        flowHandlers.put(state, handler);
    }
}