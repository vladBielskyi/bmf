package ua.vbielskyi.bmf.core.event.events;

import lombok.Data;

import java.util.UUID;

@Data
public class TenantCreatedPayload {
    private final UUID tenantId;
    private final String tenantName;
}

