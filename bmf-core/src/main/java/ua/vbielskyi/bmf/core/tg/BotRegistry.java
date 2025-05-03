package ua.vbielskyi.bmf.core.tg;

import ua.vbielskyi.bmf.core.tg.service.BotProcessorService;

import java.util.List;
import java.util.UUID;

public interface BotRegistry {

    /**
     * Register a bot processor
     *
     * @param processor The processor to register
     */
    void registerProcessor(BotProcessorService processor);

    /**
     * Find a processor that can handle this update
     *
     * @param processorType The processor type
     * @param tenantId Tenant ID
     * @return Processor or null if none found
     */
    BotProcessorService findProcessor(String processorType, UUID tenantId);

    /**
     * Get all registered processors
     *
     * @return List of processors
     */
    List<BotProcessorService> getAllProcessors();
}