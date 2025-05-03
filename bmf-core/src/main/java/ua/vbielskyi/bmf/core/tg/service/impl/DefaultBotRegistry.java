package ua.vbielskyi.bmf.core.tg.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.tg.BotRegistry;
import ua.vbielskyi.bmf.core.tg.service.BotProcessorService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class DefaultBotRegistry implements BotRegistry {

    private final List<BotProcessorService> processors = new CopyOnWriteArrayList<>();

    @Override
    public void registerProcessor(BotProcessorService processor) {
        processors.add(processor);
        log.info("Registered bot processor: {}", processor.getProcessorType());
    }

    @Override
    public BotProcessorService findProcessor(String processorType, UUID tenantId) {
        for (BotProcessorService processor : processors) {
            if (processor.canHandle(processorType, tenantId)) {
                return processor;
            }
        }
        return null;
    }

    @Override
    public List<BotProcessorService> getAllProcessors() {
        return new ArrayList<>(processors);
    }
}
