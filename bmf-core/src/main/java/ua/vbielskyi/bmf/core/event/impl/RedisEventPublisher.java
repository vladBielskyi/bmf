package ua.vbielskyi.bmf.core.event.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.event.EventPublisher;
import ua.vbielskyi.bmf.core.event.events.Event;
import ua.vbielskyi.bmf.core.event.events.OrderCreatedPayload;
import ua.vbielskyi.bmf.core.event.events.OrderStatusChangedPayload;
import ua.vbielskyi.bmf.core.event.events.PaymentFailedPayload;
import ua.vbielskyi.bmf.core.event.events.PaymentReceivedPayload;
import ua.vbielskyi.bmf.core.event.events.TenantCreatedPayload;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of EventPublisher using Redis pub/sub
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher implements EventPublisher {

    public static final String EVENT_CHANNEL_PREFIX = "bmf:events:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishTenantCreated(UUID tenantId, String tenantName) {
        TenantCreatedPayload payload = new TenantCreatedPayload(tenantId, tenantName);
        publishEvent("tenant:created", payload, tenantId);
    }

    @Override
    public void publishTenantUpdated(UUID tenantId) {
        publishEvent("tenant:updated", Map.of("tenantId", tenantId), tenantId);
    }

    @Override
    public void publishTenantActivated(UUID tenantId) {
        publishEvent("tenant:activated", Map.of("tenantId", tenantId), tenantId);
    }

    @Override
    public void publishTenantDeactivated(UUID tenantId) {
        publishEvent("tenant:deactivated", Map.of("tenantId", tenantId), tenantId);
    }

    @Override
    public void publishOrderCreated(UUID orderId, UUID tenantId, Long customerTelegramId) {
        OrderCreatedPayload payload = new OrderCreatedPayload(orderId, tenantId, customerTelegramId);
        publishEvent("order:created", payload, tenantId);
    }

    @Override
    public void publishOrderStatusChanged(UUID orderId, UUID tenantId, String previousStatus, String newStatus) {
        OrderStatusChangedPayload payload = new OrderStatusChangedPayload(
                orderId, tenantId, previousStatus, newStatus
        );
        publishEvent("order:status:changed", payload, tenantId);
    }

    @Override
    public void publishPaymentReceived(UUID orderId, UUID tenantId, String transactionId) {
        PaymentReceivedPayload payload = new PaymentReceivedPayload(orderId, tenantId, transactionId);
        publishEvent("payment:received", payload, tenantId);
    }

    @Override
    public void publishPaymentFailed(UUID orderId, UUID tenantId, String errorMessage) {
        PaymentFailedPayload payload = new PaymentFailedPayload(orderId, tenantId, errorMessage);
        publishEvent("payment:failed", payload, tenantId);
    }

    @Override
    public void publishProductCreated(UUID productId, UUID tenantId) {
        publishEvent("product:created", Map.of("productId", productId, "tenantId", tenantId), tenantId);
    }

    @Override
    public void publishProductUpdated(UUID productId, UUID tenantId) {
        publishEvent("product:updated", Map.of("productId", productId, "tenantId", tenantId), tenantId);
    }

    @Override
    public void publishProductDeleted(UUID productId, UUID tenantId) {
        publishEvent("product:deleted", Map.of("productId", productId, "tenantId", tenantId), tenantId);
    }

    @Override
    public void publishCustomerRegistered(UUID tenantId, Long customerTelegramId) {
        publishEvent("customer:registered",
                Map.of("tenantId", tenantId, "telegramId", customerTelegramId),
                tenantId);
    }

    @Override
    public void publishEvent(String eventType, Object payload, UUID tenantId) {
        try {
            String globalChannel = EVENT_CHANNEL_PREFIX + eventType;

            String tenantChannel = EVENT_CHANNEL_PREFIX + "tenant:" + tenantId + ":" + eventType;

            Event event = new Event();
            event.setEventType(eventType);
            event.setTenantId(tenantId);
            event.setTimestamp(System.currentTimeMillis());
            event.setPayload(payload);

            String jsonPayload = objectMapper.writeValueAsString(event);

            redisTemplate.convertAndSend(globalChannel, jsonPayload);
            redisTemplate.convertAndSend(tenantChannel, jsonPayload);

            log.debug("Published event: {} for tenant: {}", eventType, tenantId);
        } catch (Exception e) {
            log.error("Error publishing event: {} for tenant: {}", eventType, tenantId, e);
        }
    }
}