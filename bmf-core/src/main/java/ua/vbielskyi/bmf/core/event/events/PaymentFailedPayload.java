package ua.vbielskyi.bmf.core.event.events;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentFailedPayload {
    private final UUID orderId;
    private final UUID tenantId;
    private final String errorMessage;
}
