package ua.vbielskyi.bmf.tg.tenant.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.tg.tenant.handler.CommandHandler;
import ua.vbielskyi.bmf.tg.tenant.handler.MessageHandler;
import ua.vbielskyi.bmf.tg.tenant.handler.WebAppHandler;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Telegram bot implementation for a flower shop
 */
@Slf4j
public class FlowerShopBot { // extends TelegramLongPollingBot {

//    private final UUID tenantId;
//    private final String botUsername;
//    private final ExecutorService executorService;
//    private final CommandHandler commandHandler;
//    private final MessageHandler messageHandler;
//    private final WebAppHandler webAppHandler;
//
//    public FlowerShopBot(UUID tenantId, String botToken, String botUsername, ApplicationContext context) {
//        super(botToken);
//        this.tenantId = tenantId;
//        this.botUsername = botUsername;
//        this.executorService = Executors.newFixedThreadPool(5);
//
//        // Initialize handlers with Spring context
//        this.commandHandler = context.getBean(CommandHandler.class);
//        this.messageHandler = context.getBean(MessageHandler.class);
//        this.webAppHandler = context.getBean(WebAppHandler.class);
//
//        log.info("Initialized FlowerShopBot for tenant: {}", tenantId);
//    }
//
//    @Override
//    public String getBotUsername() {
//        return botUsername;
//    }
//
//    @Override
//    public void onUpdateReceived(Update update) {
//        executorService.submit(() -> {
//            try {
//                // Set tenant context for this thread
//                TenantContext.setCurrentTenant(tenantId);
//
//                processUpdate(update);
//            } catch (Exception e) {
//                log.error("Error processing update for tenant {}", tenantId, e);
//            } finally {
//                // Clear tenant context
//                TenantContext.clear();
//            }
//        });
//    }
//
//    private void processUpdate(Update update) {
//        try {
//            if (update.hasMessage()) {
//                if (update.getMessage().hasText()) {
//                    // Handle commands
//                    if (update.getMessage().getText().startsWith("/")) {
//                        commandHandler.handleCommand(this, update);
//                    } else {
//                        // Handle regular messages
//                        messageHandler.handleMessage(this, update);
//                    }
//                } else if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {
//                    // Handle media messages
//                    messageHandler.handleMedia(this, update);
//                }
//            } else if (update.hasCallbackQuery()) {
//                // Handle button callbacks
//                messageHandler.handleCallback(this, update);
//            } else if (update.hasWebAppData()) {
//                // Handle WebApp data
//                webAppHandler.handleWebAppData(this, update);
//            }
//        } catch (Exception e) {
//            log.error("Error in update processing for tenant {}", tenantId, e);
//
//            // Send error message to user if possible
//            if (update.hasMessage()) {
//                SendMessage errorMessage = new SendMessage();
//                errorMessage.setChatId(update.getMessage().getChatId().toString());
//                errorMessage.setText("Sorry, something went wrong. Please try again later.");
//
//                try {
//                    execute(errorMessage);
//                } catch (TelegramApiException ex) {
//                    log.error("Failed to send error message", ex);
//                }
//            }
//        }
//    }
//
//    /**
//     * Called when bot is being shutdown
//     */
//    public void onClosing() {
//        executorService.shutdown();
//        log.info("Shutting down FlowerShopBot for tenant: {}", tenantId);
//    }
}