package ua.vbielskyi.bmf.core.telegram.model;

import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.UUID;

/**
 * Unified bot message container for all types of Telegram updates
 */
@Data
@Builder
public class BotMessage {

    /**
     * Original Telegram update
     */
    private Update update;

    /**
     * Type of message
     */
    private BotMessageType type;

    /**
     * Tenant ID (null for admin bot)
     */
    private UUID tenantId;

    /**
     * Telegram chat ID
     */
    private Long chatId;

    /**
     * Telegram user ID
     */
    private Long userId;

    /**
     * Text content (if present)
     */
    private String text;

    /**
     * Command name (if message is a command)
     */
    private String command;

    /**
     * Command arguments (if message is a command)
     */
    private String[] commandArgs;

    /**
     * Callback data (if message is a callback query)
     */
    private String callbackData;

    /**
     * WebApp data (if message contains WebApp data)
     */
    private String webAppData;

    /**
     * Additional message metadata
     */
    private Map<String, Object> metadata;

    /**
     * Create a BotMessage from an Update
     */
    public static BotMessage fromUpdate(Update update, UUID tenantId) {
        BotMessageBuilder builder = BotMessage.builder()
                .update(update)
                .tenantId(tenantId);

        // Process different types of updates
        if (update.hasMessage()) {
            builder.chatId(update.getMessage().getChatId());
            builder.userId(update.getMessage().getFrom().getId());

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                builder.text(text);

                // Check if it's a command
                if (text.startsWith("/")) {
                    builder.type(BotMessageType.COMMAND);

                    // Extract command and arguments
                    String[] parts = text.split("\\s+", 2);
                    builder.command(parts[0].substring(1)); // Remove the '/'

                    if (parts.length > 1) {
                        builder.commandArgs(parts[1].split("\\s+"));
                    } else {
                        builder.commandArgs(new String[0]);
                    }
                } else {
                    builder.type(BotMessageType.TEXT);
                }
            } else if (update.getMessage().hasPhoto()) {
                builder.type(BotMessageType.PHOTO);
            } else if (update.getMessage().hasDocument()) {
                builder.type(BotMessageType.DOCUMENT);
            } else if (update.getMessage().hasLocation()) {
                builder.type(BotMessageType.LOCATION);
            } else if (update.getMessage().hasContact()) {
                builder.type(BotMessageType.CONTACT);
            } else if (update.getMessage().hasSticker()) {
                builder.type(BotMessageType.STICKER);
            } else if (update.getMessage().hasVoice()) {
                builder.type(BotMessageType.VOICE);
            } else {
                builder.type(BotMessageType.UNKNOWN);
            }

            // Check for WebApp data
            if (update.getMessage().getWebAppData() != null) {
                builder.type(BotMessageType.WEBAPP_DATA);
                builder.webAppData(update.getMessage().getWebAppData().getData());
            }
        } else if (update.hasCallbackQuery()) {
            builder.type(BotMessageType.CALLBACK_QUERY);
            builder.callbackData(update.getCallbackQuery().getData());
            builder.userId(update.getCallbackQuery().getFrom().getId());

            if (update.getCallbackQuery().getMessage() != null) {
                builder.chatId(update.getCallbackQuery().getMessage().getChatId());
            }
        } else {
            builder.type(BotMessageType.UNKNOWN);
        }

        return builder.build();
    }
}