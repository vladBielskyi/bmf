package ua.vbielskyi.bmf.core.event.events;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderStatusChangedPayload {
    private final UUID orderId;
    private final UUID tenantId;
    private final String previousStatus;
    private final String newStatus;
}
