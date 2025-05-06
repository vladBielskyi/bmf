package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.telegram.model.BotType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending notifications to admin users via admin bot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

   // private final AdminNotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Send a tenant registration notification
     */
    public void sendTenantRegistrationNotification(Long telegramId, UUID tenantId, String tenantName) {
        String message = String.format(
                "🎉 New tenant registered!\n\n" +
                        "Tenant: %s\n" +
                        "ID: %s\n\n" +
                        "Use /tenant %s to view details.",
                tenantName, tenantId, tenantId);

        sendAdminNotification(telegramId, "New Tenant Registration", message, "tenant_registration", tenantId);
    }

    /**
     * Send a subscription expiring notification
     */
    public void sendSubscriptionExpiringNotification(Long telegramId, Map<String, Object> data) {
        UUID subscriptionId = (UUID) data.get("subscriptionId");
        String plan = data.get("plan").toString();
        LocalDateTime expiryDate = (LocalDateTime) data.get("expiryDate");
        long daysRemaining = (long) data.get("daysRemaining");

        String message = String.format(
                "⚠️ Your subscription is expiring soon!\n\n" +
                        "Plan: %s\n" +
                        "Expiry Date: %s\n" +
                        "Days Remaining: %d\n\n" +
                        "Use /renew %s to renew your subscription.",
                plan, expiryDate.toLocalDate(), daysRemaining, subscriptionId);

        sendAdminNotification(telegramId, "Subscription Expiring", message, "subscription_expiring", subscriptionId);
    }

    /**
     * Send a subscription expired notification
     */
    public void sendSubscriptionExpiredNotification(Long telegramId, Map<String, Object> data) {
        UUID subscriptionId = (UUID) data.get("subscriptionId");
        String previousPlan = data.get("previousPlan").toString();
        String newPlan = data.get("newPlan").toString();

        String message = String.format(
                "❗ Your subscription has expired!\n\n" +
                        "Previous Plan: %s\n" +
                        "Current Plan: %s (FREE)\n\n" +
                        "Some features may be limited. Use /renew %s to upgrade your subscription.",
                previousPlan, newPlan, subscriptionId);

        sendAdminNotification(telegramId, "Subscription Expired", message, "subscription_expired", subscriptionId);
    }

    /**
     * Send a subscription cancelled notification
     */
    public void sendSubscriptionCancelledNotification(Long telegramId, Map<String, Object> data) {
        UUID subscriptionId = (UUID) data.get("subscriptionId");
        String plan = data.get("plan").toString();
        String reason = (String) data.get("cancellationReason");

        String message = String.format(
                "Your subscription has been cancelled.\n\n" +
                        "Plan: %s\n" +
                        "Reason: %s\n\n" +
                        "Use /resubscribe to start a new subscription.",
                plan, reason);

        sendAdminNotification(telegramId, "Subscription Cancelled", message, "subscription_cancelled", subscriptionId);
    }

    /**
     * Send a notification about a large order
     */
    public void sendLargeOrderNotification(Long telegramId, UUID tenantId,
                                           UUID orderId, String orderNumber,
                                           String customerName, String amount) {
        String message = String.format(
                "💰 Large order received!\n\n" +
                        "Order: #%s\n" +
                        "Customer: %s\n" +
                        "Amount: %s\n\n" +
                        "Use /order %s to view details.",
                orderNumber, customerName, amount, orderId);

        sendAdminNotification(telegramId, "Large Order Received", message, "large_order", orderId);
    }

    /**
     * Send a notification about low inventory
     */
    public void sendLowInventoryNotification(Long telegramId, UUID tenantId,
                                             UUID productId, String productName,
                                             int currentStock, int threshold) {
        String message = String.format(
                "⚠️ Low inventory alert!\n\n" +
                        "Product: %s\n" +
                        "Current Stock: %d\n" +
                        "Threshold: %d\n\n" +
                        "Use /product %s to view details.",
                productName, currentStock, threshold, productId);

        sendAdminNotification(telegramId, "Low Inventory Alert", message, "low_inventory", productId);
    }

    /**
     * Send a notification about a customer support request
     */
    public void sendSupportRequestNotification(Long telegramId, UUID tenantId,
                                               UUID customerId, String customerName,
                                               String message) {
        String notificationMessage = String.format(
                "🆘 New customer support request!\n\n" +
                        "Customer: %s\n\n" +
                        "Message: %s\n\n" +
                        "Use /support %s to respond.",
                customerName, message, customerId);

        sendAdminNotification(telegramId, "Customer Support Request",
                notificationMessage, "support_request", customerId);
    }

    /**
     * Send an admin notification
     */
    private void sendAdminNotification(Long telegramId, String title,
                                       String message, String type, UUID referenceId) {
        try {
            // Save notification to database
//            AdminNotificationEntity notification = new AdminNotificationEntity();
//            notification.setUserId(telegramId);
//            notification.setTitle(title);
//            notification.setMessage(message);
//            notification.setType(type);
//            notification.setReferenceId(referenceId);
//            notification.setRead(false);
//            notification.setCreatedAt(LocalDateTime.now());
//
//            notificationRepository.save(notification);
//
//            // Send notification via Telegram admin bot
//            notificationService.sendAdminBotNotification(telegramId, title, message);
//
//            log.info("Sent admin notification to {}: type={}, referenceId={}",
//                    telegramId, type, referenceId);
        } catch (Exception e) {
            log.error("Error sending admin notification to {}", telegramId, e);
        }
    }

//    /**
//     * Get unread notifications for a user
//     */
//    public List<AdminNotificationEntity> getUnreadNotifications(Long telegramId) {
//        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(telegramId);
//    }
//
//    /**
//     * Mark notification as read
//     */
//    public void markAsRead(UUID notificationId) {
//        AdminNotificationEntity notification = notificationRepository.findById(notificationId)
//                .orElse(null);
//
//        if (notification != null) {
//            notification.setRead(true);
//            notification.setReadAt(LocalDateTime.now());
//            notificationRepository.save(notification);
//        }
//    }
}