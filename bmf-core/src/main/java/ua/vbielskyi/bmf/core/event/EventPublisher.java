package ua.vbielskyi.bmf.core.event;

import java.util.UUID;

/**
 * Interface for publishing events in the system
 * This allows for decoupling between components and easier transition to microservices
 */
public interface EventPublisher {

    /**
     * Publish a tenant created event
     * @param tenantId The ID of the created tenant
     * @param tenantName The name of the created tenant
     */
    void publishTenantCreated(UUID tenantId, String tenantName);

    /**
     * Publish a tenant updated event
     * @param tenantId The ID of the updated tenant
     */
    void publishTenantUpdated(UUID tenantId);

    /**
     * Publish a tenant activated event
     * @param tenantId The ID of the activated tenant
     */
    void publishTenantActivated(UUID tenantId);

    /**
     * Publish a tenant deactivated event
     * @param tenantId The ID of the deactivated tenant
     */
    void publishTenantDeactivated(UUID tenantId);

    /**
     * Publish an order created event
     * @param orderId The ID of the created order
     * @param tenantId The tenant ID
     * @param customerTelegramId The customer's Telegram ID
     */
    void publishOrderCreated(UUID orderId, UUID tenantId, Long customerTelegramId);

    /**
     * Publish an order status changed event
     * @param orderId The ID of the order
     * @param tenantId The tenant ID
     * @param previousStatus Previous order status
     * @param newStatus New order status
     */
    void publishOrderStatusChanged(UUID orderId, UUID tenantId, String previousStatus, String newStatus);

    /**
     * Publish a payment received event
     * @param orderId The ID of the order
     * @param tenantId The tenant ID
     * @param transactionId The payment transaction ID
     */
    void publishPaymentReceived(UUID orderId, UUID tenantId, String transactionId);

    /**
     * Publish a payment failed event
     * @param orderId The ID of the order
     * @param tenantId The tenant ID
     * @param errorMessage Error message
     */
    void publishPaymentFailed(UUID orderId, UUID tenantId, String errorMessage);

    /**
     * Publish a product created event
     * @param productId The ID of the created product
     * @param tenantId The tenant ID
     */
    void publishProductCreated(UUID productId, UUID tenantId);

    /**
     * Publish a product updated event
     * @param productId The ID of the updated product
     * @param tenantId The tenant ID
     */
    void publishProductUpdated(UUID productId, UUID tenantId);

    /**
     * Publish a product deleted event
     * @param productId The ID of the deleted product
     * @param tenantId The tenant ID
     */
    void publishProductDeleted(UUID productId, UUID tenantId);

    /**
     * Publish a customer registered event
     * @param tenantId The tenant ID
     * @param customerTelegramId The customer's Telegram ID
     */
    void publishCustomerRegistered(UUID tenantId, Long customerTelegramId);

    /**
     * Publish a generic event
     * @param eventType Event type
     * @param payload Event payload
     * @param tenantId The tenant ID
     */
    void publishEvent(String eventType, Object payload, UUID tenantId);
}