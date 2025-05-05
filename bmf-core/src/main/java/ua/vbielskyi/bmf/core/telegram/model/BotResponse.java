package ua.vbielskyi.bmf.core.telegram.model;

import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified response container for all types of bot responses
 */
@Data
@Builder
public class BotResponse {

    /**
     * Primary response method (can be SendMessage, EditMessageText, etc.)
     */
    private BotApiMethod<?> method;

    /**
     * Additional response methods to be executed
     */
    @Builder.Default
    private List<Object> additionalMethods = new ArrayList<>();

    /**
     * Create a simple text response
     */
    public static BotResponse text(Long chatId, String text) {
        return BotResponse.builder()
                .method(SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(text)
                        .build())
                .build();
    }

    /**
     * Create an edit message response
     */
    public static BotResponse editText(Long chatId, Integer messageId, String text) {
        return BotResponse.builder()
                .method(EditMessageText.builder()
                        .chatId(chatId.toString())
                        .messageId(messageId)
                        .text(text)
                        .build())
                .build();
    }

    /**
     * Add a method to be executed
     */
    public BotResponse addMethod(Object method) {
        this.additionalMethods.add(method);
        return this;
    }
}