//package ua.vbielskyi.bmf.tg.admin.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
//import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
//import ua.vbielskyi.bmf.core.entity.tenant.TenantOwnerEntity;
//import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
//import ua.vbielskyi.bmf.core.repository.tenant.TenantOwnerRepository;
//import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
//import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
//import ua.vbielskyi.bmf.core.telegram.model.BotType;
//import ua.vbielskyi.bmf.core.telegram.service.BotExecutor;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AdminNotificationService {
//
//    //private final AdminNotificationRepository notificationRepository;
//    private final TenantOwnerRepository tenantOwnerRepository;
//    private final TenantRepository tenantRepository;
//    private final OrderRepository orderRepository;
//    private final BotExecutor botExecutor;
//    private final LocalizationService localizationService;
//
//    /**
//     * Send notification to tenant owner
//     */
//    public void sendNotification(Long userId, String title, String message, String type, UUID referenceId) {
//        try {
//            // Save notification to database
//            AdminNotificationEntity notification = new AdminNotificationEntity();
//            notification.setUserId(userId);
//            notification.setTitle(title);
//            notification.setMessage(message);
//            notification.setType(type);
//            notification.setReferenceId(referenceId);
//            notification.setRead(false);
//            notification.setCreatedAt(LocalDateTime.now());
//
//            notificationRepository.save(notification);
//
//            // Send Telegram notification
//            SendMessage sendMessage = SendMessage.builder()
//                    .chatId(userId.toString())
//                    .text("*" + title + "*\n\n" + message)
//                    .parseMode("Markdown")
//                    .build();
//
//            BotResponse response = BotResponse.builder()
//                    .method(sendMessage)
//                    .build();
//
//            // Execute via admin bot
//            botExecutor.executeAsync(response, BotType.ADMIN, null);
//
//            log.info("Sent notification to user {}: {}", userId, title);
//        } catch (Exception e) {
//            log.error("Error sending notification to user {}", userId, e);
//        }
//    }
//
//    /**
//     * Get unread notifications for a user
//     */
//    public List<AdminNotificationEntity> getUnreadNotifications(Long userId) {
//        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
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
//
//    /**
//     * Send new order notification
//     */
//    public void sendNewOrderNotification(OrderEntity order) {
//        try {
//            UUID tenantId = order.getTenantId();
//
//            // Get shop owners
//            List<TenantOwnerEntity> owners = tenantOwnerRepository.findAllByTenantId(tenantId);
//            TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
//
//            if (tenant == null || owners.isEmpty()) {
//                return;
//            }
//
//            String shopName = tenant.getShopName();
//            String orderNumber = order.getOrderNumber();
//
//            for (TenantOwnerEntity owner : owners) {
//                Long userId = owner.getUserId();
//
//                // Get user's language
//                String language = localizationService.getUserLanguage(userId);
//
//                // Prepare notification
//                String title = localizationService.getMessageWithLanguage("notification.new_order", language, shopName);
//                String message = localizationService.getMessageWithLanguage(
//                        "notification.new_order.details", language,
//                        orderNumber,
//                        order.getFinalAmount().toString(),
//                        order.getCreatedAt().toString()
//                );
//
//                sendNotification(userId, title, message, "new_order", order.getId());
//            }
//        } catch (Exception e) {
//            log.error("Error sending new order notification", e);
//        }
//    }
//
//    /**
//     * Send subscription expiring notification
//     */
//    public void sendSubscriptionExpiryNotification(TenantEntity tenant, long daysRemaining) {
//        try {
//            UUID tenantId = tenant.getId();
//
//            // Get shop owners
//            List<TenantOwnerEntity> owners = tenantOwnerRepository.findAllByTenantId(tenantId);
//
//            if (owners.isEmpty()) {
//                return;
//            }
//
//            String shopName = tenant.getShopName();
//
//            for (TenantOwnerEntity owner : owners) {
//                Long userId = owner.getUserId();
//
//                // Get user's language
//                String language = localizationService.getUserLanguage(userId);
//
//                // Prepare notification
//                String title = localizationService.getMessageWithLanguage(
//                        "notification.subscription_expiring", language, shopName);
//
//                String message = localizationService.getMessageWithLanguage(
//                        "notification.subscription_expiring.details", language,
//                        shopName,
//                        tenant.getSubscriptionPlan().toString(),
//                        String.valueOf(daysRemaining),
//                        tenant.getSubscriptionExpiryDate().toString()
//                );
//
//                sendNotification(userId, title, message, "subscription_expiring", tenantId);
//            }
//        } catch (Exception e) {
//            log.error("Error sending subscription expiry notification", e);
//        }
//    }
//
//    /**
//     * Check for expiring subscriptions daily
//     */
//    @Scheduled(cron = "0 0 9 * * ?") // Run at 9 AM daily
//    public void checkExpiringSubscriptions() {
//        log.info("Checking for expiring subscriptions");
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime sevenDaysLater = now.plusDays(7);
//
//        // Find tenants with subscriptions expiring in 7 days
//        List<TenantEntity> expiringTenants = tenantRepository.findAll().stream()
//                .filter(t -> t.getSubscriptionExpiryDate() != null &&
//                        t.getSubscriptionExpiryDate().isAfter(now) &&
//                        t.getSubscriptionExpiryDate().isBefore(sevenDaysLater))
//                .toList();
//
//        for (TenantEntity tenant : expiringTenants) {
//            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
//                    now.toLocalDate(), tenant.getSubscriptionExpiryDate().toLocalDate());
//
//            sendSubscriptionExpiryNotification(tenant, daysRemaining);
//        }
//    }
//}