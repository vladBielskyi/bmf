package ua.vbielskyi.bmf.core.telegram.service;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;

/**
 * Interface for processing bot updates
 * This is the core interface that all bot modules will implement
 */
public interface BotProcessorService {

    /**
     * Unique identifier for this bot processor type
     */
    String getProcessorType();

    /**
     * Process a Telegram update
     *
     * @param update The update to process
     * @param tenantId Optional tenant ID (can be null for admin bot)
     * @return Response to send back
     */
    BotApiMethod<?> processUpdate(Update update, UUID tenantId);

    /**
     * Check if this processor can handle this type of update
     *
     * @param processorType The processor type
     * @param tenantId Tenant ID
     * @return True if this processor can handle this update
     */
    boolean canHandle(String processorType, UUID tenantId);
}
