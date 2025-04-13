package ua.vbielskyi.bmf.common.model.order;

import lombok.Getter;

/**
 * Payment method enum
 */
@Getter
public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on delivery"),
    CREDIT_CARD("Credit card"),
    TELEGRAM_PAYMENTS("Telegram payments"),
    BANK_TRANSFER("Bank transfer");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

}