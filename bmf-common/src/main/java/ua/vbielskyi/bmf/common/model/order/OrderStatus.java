package ua.vbielskyi.bmf.common.model.order;

import lombok.Getter;

/**
 * Represents the possible statuses of an order
 */
@Getter
public enum OrderStatus {
    NEW("New order received"),
    CONFIRMED("Order confirmed"),
    PROCESSING("Order is being processed"),
    READY_FOR_DELIVERY("Order is ready for delivery"),
    OUT_FOR_DELIVERY("Order is out for delivery"),
    DELIVERED("Order has been delivered"),
    COMPLETED("Order completed successfully"),
    CANCELLED("Order has been cancelled"),
    REFUNDED("Order has been refunded");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

}