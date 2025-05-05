package ua.vbielskyi.bmf.common.model.order;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("Payment pending"),
    PROCESSING("Payment processing"),
    COMPLETED("Payment completed"),
    FAILED("Payment failed"),
    REFUNDED("Payment refunded");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

}
