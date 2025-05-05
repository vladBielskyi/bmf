package ua.vbielskyi.bmf.core.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for executing bot responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotExecutor {

    private final CachedBotRegistry botRegistry;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Execute a BotResponse asynchronously
     *
     * @param response Bot response to execute
     * @param botType Bot type
     * @param tenantId Tenant ID (null for admin bot)
     */
    public void executeAsync(BotResponse response, BotType botType, UUID tenantId) {
        executorService.submit(() -> {
            try {
                execute(response, botType, tenantId);
            } catch (Exception e) {
                log.error("Error executing bot response for tenant {}", tenantId, e);
            }
        });
    }

    /**
     * Execute a BotResponse
     *
     * @param response Bot response to execute
     * @param botType Bot type
     * @param tenantId Tenant ID (null for admin bot)
     * @throws TelegramApiException If execution fails
     */
    public void execute(BotResponse response, BotType botType, UUID tenantId) throws TelegramApiException {
        // Get bot token
        CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(botType, tenantId);
        if (config == null) {
            log.error("No bot configuration found for botType: {}, tenantId: {}", botType, tenantId);
            return;
        }

        String botToken = config.getToken();

        // Execute primary method
        executeBotMethod(response.getMethod(), botToken);

        // Execute additional methods
        for (Object method : response.getAdditionalMethods()) {
            executeBotMethod(method, botToken);
        }
    }

    /**
     * Execute a single bot method
     *
     * @param method Method to execute
     * @param botToken Bot token
     * @throws TelegramApiException If execution fails
     */
    private void executeBotMethod(Object method, String botToken) throws TelegramApiException {
        // Use Telegram API client to execute the method
        // The implementation would depend on how you're interacting with Telegram API
        // This is a simplified example

        if (method instanceof BotApiMethod) {
            // Execute BotApiMethod
            BotApiMethod<?> apiMethod = (BotApiMethod<?>) method;
            // botClient.execute(apiMethod, botToken);
            log.debug("Executed bot method: {}", apiMethod.getMethod());
        } else if (method instanceof SendMessage) {
            // Execute SendMessage
            SendMessage sendMessage = (SendMessage) method;
            // botClient.execute(sendMessage, botToken);
            log.debug("Sent message to chat: {}", sendMessage.getChatId());
        } else if (method instanceof SendPhoto) {
            // Execute SendPhoto
            SendPhoto sendPhoto = (SendPhoto) method;
            // botClient.execute(sendPhoto, botToken);
            log.debug("Sent photo to chat: {}", sendPhoto.getChatId());
        } else if (method instanceof EditMessageText) {
            // Execute EditMessageText
            EditMessageText editMessage = (EditMessageText) method;
            // botClient.execute(editMessage, botToken);
            log.debug("Edited message: {}", editMessage.getMessageId());
        } else {
            log.warn("Unsupported method type: {}", method.getClass().getName());
        }
    }
}