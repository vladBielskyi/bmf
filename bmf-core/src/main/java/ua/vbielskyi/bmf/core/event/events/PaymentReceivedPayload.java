package ua.vbielskyi.bmf.core.event.events;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentReceivedPayload {
    private final UUID orderId;
    private final UUID tenantId;
    private final String transactionId;
}
