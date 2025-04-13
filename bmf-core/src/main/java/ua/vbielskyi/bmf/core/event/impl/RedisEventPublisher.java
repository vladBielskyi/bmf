package ua.vbielskyi.bmf.core.event.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.event.EventPublisher;

import java.util.UUID;

/**
 * Implementation of EventPublisher using Redis pub/sub
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher implements EventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Channel prefix for all events
    private static final String EVENT_CHANNEL_PREFIX = "bmf:events:";

    @Override
    public void publishTenantCreated(UUID tenantId, String tenantName) {

    }

    @Override
    public void publishTenantUpdated(UUID tenantId) {

    }

    @Override
    public void publishTenantActivated(UUID tenantId) {

    }

    @Override
    public void publishTenantDeactivated(UUID tenantId) {

    }

    @Override
    public void publishOrderCreated(UUID orderId, UUID tenantId, Long customerTelegramId) {

    }

    @Override
    public void publishOrderStatusChanged(UUID orderId, UUID tenantId, String previousStatus, String newStatus) {

    }

    @Override
    public void publishPaymentReceived(UUID orderId, UUID tenantId, String transactionId) {

    }

    @Override
    public void publishPaymentFailed(UUID orderId, UUID tenantId, String errorMessage) {

    }

    @Override
    public void publishProductCreated(UUID productId, UUID tenantId) {

    }

    @Override
    public void publishProductUpdated(UUID productId, UUID tenantId) {

    }

    @Override
    public void publishProductDeleted(UUID productId, UUID tenantId) {

    }

    @Override
    public void publishCustomerRegistered(UUID tenantId, Long customerTelegramId) {

    }

    @Override
    public void publishEvent(String eventType, Object payload, UUID tenantId) {
        try {
            // Create global channel
            String globalChannel = EVENT_CHANNEL_PREFIX + eventType;

            // Create tenant-specific channel
            String tenantChannel = EVENT_CHANNEL_PREFIX + "tenant:" + tenantId + ":" + eventType;

            // Convert payload to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish to both channels
            redisTemplate.convertAndSend(globalChannel, jsonPayload);
            redisTemplate.convertAndSend(tenantChannel, jsonPayload);

            log.debug("Published event: {} for tenant: {}", eventType, tenantId);
        } catch (Exception e) {
            log.error("Error publishing event: {} for tenant: {}", eventType, tenantId, e);
        }
    }
}