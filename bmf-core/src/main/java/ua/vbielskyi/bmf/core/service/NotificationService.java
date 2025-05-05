package ua.vbielskyi.bmf.core.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.notification.NotificationHistoryEntity;
import ua.vbielskyi.bmf.core.entity.notification.NotificationTemplateEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.repository.notification.NotificationHistoryRepository;
import ua.vbielskyi.bmf.core.repository.notification.NotificationTemplateRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final TelegramNotificationProvider telegramNotificationProvider;
    private final EmailNotificationProvider emailNotificationProvider;
    private final SmsNotificationProvider smsNotificationProvider;

    // Notification types
    public static final String NOTIFICATION_ORDER_CONFIRMATION = "order_confirmation";
    public static final String NOTIFICATION_ORDER_STATUS_UPDATE = "order_status_update";
    public static final String NOTIFICATION_PAYMENT_CONFIRMATION = "payment_confirmation";
    public static final String NOTIFICATION_PAYMENT_FAILURE = "payment_failure";
    public static final String NOTIFICATION_DELIVERY_UPDATE = "delivery_update";
    public static final String NOTIFICATION_MARKETING = "marketing";
    public static final String NOTIFICATION_PROMOTIONAL = "promotional";

    /**
     * Send order confirmation notification
     */
    public void sendOrderConfirmation(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderDate", order.getCreatedAt());
        templateData.put("totalAmount", order.getFinalAmount());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_ORDER_CONFIRMATION,
                templateData,
                order.getId()
        );
    }

    /**
     * Send order processing update notification
     */
    public void sendOrderProcessingUpdate(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_ORDER_STATUS_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send order ready notification
     */
    public void sendOrderReadyNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_ORDER_STATUS_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send out for delivery notification
     */
    public void sendOutForDeliveryNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());
        templateData.put("deliveryAddress", order.getDeliveryAddress());
        templateData.put("deliveryTime", order.getDeliveryTime());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_DELIVERY_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send delivery completed notification
     */
    public void sendDeliveryCompletedNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_DELIVERY_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send order completed notification
     */
    public void sendOrderCompletedNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());
        templateData.put("totalAmount", order.getFinalAmount());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_ORDER_STATUS_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send feedback request
     */
    public void sendFeedbackRequest(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("customerName", order.getCustomerName());

        // Use promotional template type for feedback requests
        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_PROMOTIONAL,
                templateData,
                order.getId()
        );
    }

    /**
     * Send cancellation notification
     */
    public void sendCancellationNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_ORDER_STATUS_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send refund notification
     */
    public void sendRefundNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("orderStatus", order.getStatus().getDescription());
        templateData.put("customerName", order.getCustomerName());
        templateData.put("refundAmount", order.getFinalAmount());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_PAYMENT_CONFIRMATION,
                templateData,
                order.getId()
        );
    }

    /**
     * Send payment confirmation
     */
    public void sendPaymentConfirmation(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("paymentAmount", order.getFinalAmount());
        templateData.put("paymentMethod", order.getPaymentMethod().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_PAYMENT_CONFIRMATION,
                templateData,
                order.getId()
        );
    }

    /**
     * Send payment failure notification
     */
    public void sendPaymentFailureNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("paymentAmount", order.getFinalAmount());
        templateData.put("paymentMethod", order.getPaymentMethod().getDescription());
        templateData.put("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_PAYMENT_FAILURE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send delivery update notification
     */
    public void sendDeliveryUpdateNotification(OrderEntity order, Map<String, Object> templateData) {
        // Add default template values
        templateData.putIfAbsent("orderNumber", order.getOrderNumber());
        templateData.putIfAbsent("customerName", order.getCustomerName());

        sendNotification(
                order.getTenantId(),
                order.getCustomerTelegramId(),
                NOTIFICATION_DELIVERY_UPDATE,
                templateData,
                order.getId()
        );
    }

    /**
     * Send promotional/marketing notification
     */
    public void sendPromotionalNotification(UUID tenantId, Long telegramId, String title, String message) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("title", title);
        templateData.put("message", message);

        sendNotification(
                tenantId,
                telegramId,
                NOTIFICATION_MARKETING,
                templateData,
                null
        );
    }

    /**
     * Generic method to send notification
     */
    private void sendNotification(UUID tenantId, Long telegramId, String notificationType,
                                  Map<String, Object> templateData, UUID orderId) {
        try {
            // Get template for this notification type
            NotificationTemplateEntity template = notificationTemplateRepository
                    .findByTenantIdAndTypeAndActiveTrue(tenantId, notificationType)
                    .orElse(null);

            if (template == null) {
                log.warn("No active template found for notification type: {}, tenant: {}",
                        notificationType, tenantId);
                return;
            }

            // Process template with variables
            String messageContent = processTemplate(template.getMessageTemplate(), templateData);

            // Create notification history record
            NotificationHistoryEntity history = new NotificationHistoryEntity();
            history.setTenantId(tenantId);
            history.setTelegramId(telegramId);
            history.setOrderId(orderId);
            history.setType(notificationType);
            history.setTemplateId(template.getId());
            history.setMessage(messageContent);
            history.setSent(false);
            history.setCreatedAt(LocalDateTime.now());

            history = notificationHistoryRepository.save(history);

            // Send notification via Telegram bot
            boolean sent = telegramNotificationProvider.sendNotification(
                    tenantId, telegramId, messageContent);

            // Update notification history
            if (sent) {
                history.setSent(true);
                history.setSentAt(LocalDateTime.now());
            } else {
                history.setErrorMessage("Failed to send via Telegram");
            }

            notificationHistoryRepository.save(history);

        } catch (Exception e) {
            log.error("Error sending notification type: {} to: {}", notificationType, telegramId, e);
        }
    }

    /**
     * Process template with variables
     */
    private String processTemplate(String template, Map<String, Object> data) {
        String result = template;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue().toString() : "");
        }

        return result;
    }
}