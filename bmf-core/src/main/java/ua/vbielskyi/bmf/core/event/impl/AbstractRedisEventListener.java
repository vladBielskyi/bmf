package ua.vbielskyi.bmf.core.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.event.RedisEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
abstract class AbstractRedisEventListener implements RedisEventListener {
    private final ObjectMapper objectMapper;
    private final List<String> eventTypes;
    private final String description;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channelName = new String(pattern);
            String content = new String(message.getBody());

            // Parse the event
            JsonNode eventNode = objectMapper.readTree(content);
            String eventType = eventNode.get("eventType").asText();

            // Check if we should handle this event type
            if (eventTypes.contains(eventType)) {
                UUID tenantId = UUID.fromString(eventNode.get("tenantId").asText());
                JsonNode payload = eventNode.get("payload");

                // Process the event
                processEvent(eventType, tenantId, payload);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }

    /**
     * Process the received event
     * @param eventType Event type
     * @param tenantId Tenant ID
     * @param payload Event payload as JsonNode
     */
    protected abstract void processEvent(String eventType, UUID tenantId, JsonNode payload) throws JsonProcessingException;

    @Override
    public Collection<ChannelTopic> getChannels() {
        List<ChannelTopic> channels = new ArrayList<>();

        // Subscribe to all events
        channels.add(new ChannelTopic(RedisEventPublisher.EVENT_CHANNEL_PREFIX + "*"));

        return channels;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
