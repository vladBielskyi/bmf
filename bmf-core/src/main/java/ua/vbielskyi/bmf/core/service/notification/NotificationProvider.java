package ua.vbielskyi.bmf.core.service.notification;

import java.util.UUID;

/**
 * Interface for notification providers
 */
public interface NotificationProvider {

    /**
     * Send notification
     *
     * @param tenantId Tenant ID
     * @param recipientId Recipient identifier (e.g., Telegram ID)
     * @param message Message content
     * @return true if notification was sent successfully
     */
    boolean sendNotification(UUID tenantId, Long recipientId, String message);
}