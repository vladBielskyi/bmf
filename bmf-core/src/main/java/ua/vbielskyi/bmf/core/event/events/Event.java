package ua.vbielskyi.bmf.core.event.events;

import lombok.Data;

import java.util.UUID;

@Data
public class Event {
    private String eventType;
    private UUID tenantId;
    private Long timestamp;
    private Object payload;
}
