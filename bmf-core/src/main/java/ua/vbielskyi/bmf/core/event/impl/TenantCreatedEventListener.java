package ua.vbielskyi.bmf.core.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TenantCreatedEventListener extends AbstractRedisEventListener{

    public TenantCreatedEventListener(ObjectMapper objectMapper, List<String> eventTypes, String description) {
        super(objectMapper, eventTypes, description);
    }

    @Override
    protected void processEvent(String eventType, UUID tenantId, JsonNode payload) throws JsonProcessingException {

    }
}
