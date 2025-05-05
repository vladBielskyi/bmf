package ua.vbielskyi.bmf.core.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotExecutor;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;

import java.util.UUID;

/**
 * Provider for sending notifications via Telegram bot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationProvider implements NotificationProvider {

    private final BotExecutor botExecutor;
    private final CachedBotRegistry botRegistry;

    /**
     * Send notification via Telegram bot
     */
    public boolean sendNotification(UUID tenantId, Long chatId, String message) {
        try {
            // Verify bot is registered and active
            CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(BotType.TENANT, tenantId);
            if (config == null || !config.isActive()) {
                log.error("No active bot configuration found for tenant: {}", tenantId);
                return false;
            }

            // Create message
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(message)
                    .parseMode("HTML")
                    .disableWebPagePreview(true)
                    .build();

            // Execute message
            botExecutor.executeAsync(
                    org.telegram.telegrambots.meta.api.methods.BotApiMethod.builder().build(),
                    BotType.TENANT, tenantId);

            log.debug("Sent Telegram notification to {} for tenant {}", chatId, tenantId);
            return true;
        } catch (Exception e) {
            log.error("Error sending Telegram notification to {} for tenant {}", chatId, tenantId, e);
            return false;
        }
    }

    /**
     * Send notification with image
     */
    public boolean sendNotificationWithImage(UUID tenantId, Long chatId, String message, String imageUrl) {
        // Implementation for sending message with image
        // Would use SendPhoto instead of SendMessage
        return false;
    }

    /**
     * Send notification with buttons
     */
    public boolean sendNotificationWithButtons(UUID tenantId, Long chatId, String message,
                                               Map<String, String> buttons) {
        // Implementation for sending message with inline keyboard buttons
        return false;
    }
}