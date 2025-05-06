package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.subscription.SubscriptionStatus;
import ua.vbielskyi.bmf.common.model.tenant.SubscriptionPlan;
import ua.vbielskyi.bmf.core.entity.subscription.SubscriptionEntity;
import ua.vbielskyi.bmf.core.entity.subscription.SubscriptionPaymentEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantOwnerEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.subscription.SubscriptionPaymentRepository;
import ua.vbielskyi.bmf.core.repository.subscription.SubscriptionRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantOwnerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
//import ua.vbielskyi.bmf.core.service.notification.AdminNotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;
    private final TenantOwnerRepository tenantOwnerRepository;
   // private final AdminNotificationService adminNotificationService;

    // Subscription plan pricing in USD
    private static final Map<SubscriptionPlan, BigDecimal> PLAN_PRICES = Map.of(
            SubscriptionPlan.FREE, BigDecimal.ZERO,
            SubscriptionPlan.BASIC, new BigDecimal("19.99"),
            SubscriptionPlan.PREMIUM, new BigDecimal("49.99"),
            SubscriptionPlan.ENTERPRISE, new BigDecimal("99.99")
    );

    /**
     * Create or update subscription for a tenant
     */
    @Transactional
    public SubscriptionEntity createOrUpdateSubscription(UUID tenantId, SubscriptionPlan plan, int durationMonths) {
        // Validate tenant
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        // Calculate dates
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime endDate = now.plusMonths(durationMonths);

        // Check if tenant already has a subscription
        SubscriptionEntity subscription = subscriptionRepository.findByTenantIdAndStatusNot(
                        tenantId, SubscriptionStatus.CANCELLED)
                .orElse(null);

        if (subscription != null) {
            // Extend existing subscription
            if (subscription.getEndDate().isAfter(now)) {
                // If subscription is still active, extend from current end date
                startDate = subscription.getEndDate();
                endDate = startDate.plusMonths(durationMonths);
            }

            subscription.setPlan(plan);
            subscription.setEndDate(endDate);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setUpdatedAt(now);
        } else {
            // Create new subscription
            subscription = new SubscriptionEntity();
            subscription.setTenantId(tenantId);
            subscription.setPlan(plan);
            subscription.setStartDate(startDate);
            subscription.setEndDate(endDate);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCreatedAt(now);
            subscription.setUpdatedAt(now);
        }

        SubscriptionEntity savedSubscription = subscriptionRepository.save(subscription);

        // Update tenant with subscription plan
        tenant.setSubscriptionPlan(plan);
        tenant.setSubscriptionExpiryDate(endDate);
        tenant.setUpdatedAt(now);
        tenantRepository.save(tenant);

        log.info("Created/updated subscription for tenant {}: plan={}, endDate={}",
                tenantId, plan, endDate);

        return savedSubscription;
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(UUID subscriptionId, String reason) {
        SubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        LocalDateTime now = LocalDateTime.now();

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason(reason);
        subscription.setCancelledAt(now);
        subscription.setUpdatedAt(now);

        subscriptionRepository.save(subscription);

        log.info("Cancelled subscription {}, reason: {}", subscriptionId, reason);

        // Notify tenant owners
        notifySubscriptionCancellation(subscription);
    }

    /**
     * Record subscription payment
     */
    @Transactional
    public SubscriptionPaymentEntity recordPayment(UUID subscriptionId, BigDecimal amount,
                                                   String paymentMethod, String transactionId) {
        SubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        LocalDateTime now = LocalDateTime.now();

        SubscriptionPaymentEntity payment = new SubscriptionPaymentEntity();
        payment.setSubscriptionId(subscriptionId);
        payment.setTenantId(subscription.getTenantId());
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setStatus("COMPLETED");
        payment.setCreatedAt(now);

        SubscriptionPaymentEntity savedPayment = paymentRepository.save(payment);

        log.info("Recorded payment for subscription {}: amount={}, method={}",
                subscriptionId, amount, paymentMethod);

        return savedPayment;
    }

    /**
     * Get subscription price for plan and duration
     */
    public BigDecimal calculateSubscriptionPrice(SubscriptionPlan plan, int durationMonths) {
        BigDecimal monthlyPrice = PLAN_PRICES.getOrDefault(plan, BigDecimal.ZERO);

        // Apply discounts for longer durations
        BigDecimal discountMultiplier = BigDecimal.ONE;
        if (durationMonths >= 12) {
            // 20% discount for annual subscriptions
            discountMultiplier = new BigDecimal("0.8");
        } else if (durationMonths >= 6) {
            // 10% discount for 6-month subscriptions
            discountMultiplier = new BigDecimal("0.9");
        }

        return monthlyPrice.multiply(BigDecimal.valueOf(durationMonths))
                .multiply(discountMultiplier)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Check tenant's subscription limits
     */
    public boolean isWithinSubscriptionLimits(UUID tenantId, String resourceType, int currentCount) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        SubscriptionPlan plan = tenant.getSubscriptionPlan();

        // Check limits based on resource type
        switch (resourceType) {
            case "products":
                return currentCount < plan.getMaxProducts();
            case "admin_users":
                return currentCount < plan.getMaxAdminUsers();
            case "orders":
                // For orders, we check against monthly limits
                return countMonthlyOrders(tenantId) < plan.getMaxOrders();
            default:
                log.warn("Unknown resource type for subscription limit check: {}", resourceType);
                return true;
        }
    }

    /**
     * Count orders created this month
     */
    private int countMonthlyOrders(UUID tenantId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        // This would typically use a repository method to count orders
        // For demonstration, returning a placeholder value
        return 0;
    }

    /**
     * Scheduled task to check and notify about expiring subscriptions
     */
    @Scheduled(cron = "0 0 8 * * ?") // Run at 8 AM every day
    public void checkExpiringSubscriptions() {
        log.info("Checking for expiring subscriptions");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);

        // Find subscriptions expiring in the next 7 days
        List<SubscriptionEntity> expiringSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, now, sevenDaysFromNow);

        for (SubscriptionEntity subscription : expiringSubscriptions) {
            // Send notification about expiring subscription
            notifySubscriptionExpiring(subscription);
        }

        // Find expired subscriptions
        List<SubscriptionEntity> expiredSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        for (SubscriptionEntity subscription : expiredSubscriptions) {
            handleExpiredSubscription(subscription);
        }
    }

    /**
     * Handle expired subscription
     */
    @Transactional
    protected void handleExpiredSubscription(SubscriptionEntity subscription) {
        UUID tenantId = subscription.getTenantId();

        // Downgrade to FREE plan
        if (subscription.getPlan() != SubscriptionPlan.FREE) {
            // Update subscription
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);

            // Create new FREE subscription
            createOrUpdateSubscription(tenantId, SubscriptionPlan.FREE, 12);

            // Send notification about expired subscription
            notifySubscriptionExpired(subscription);

            log.info("Subscription {} expired for tenant {}, downgraded to FREE plan",
                    subscription.getId(), tenantId);
        }
    }

    /**
     * Notify about expiring subscription
     */
    private void notifySubscriptionExpiring(SubscriptionEntity subscription) {
        UUID tenantId = subscription.getTenantId();

        // Get tenant owners
        List<TenantOwnerEntity> owners = tenantOwnerRepository.findAllByTenantId(tenantId);

        for (TenantOwnerEntity owner : owners) {
            // Calculate days until expiration
            long daysUntilExpiration = ChronoUnit.DAYS.between(
                    LocalDateTime.now(), subscription.getEndDate());

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("subscriptionId", subscription.getId());
            notificationData.put("plan", subscription.getPlan());
            notificationData.put("expiryDate", subscription.getEndDate());
            notificationData.put("daysRemaining", daysUntilExpiration);

//            adminNotificationService.sendSubscriptionExpiringNotification(
//                    owner.getUserId(), notificationData);
        }
    }

    /**
     * Notify about expired subscription
     */
    private void notifySubscriptionExpired(SubscriptionEntity subscription) {
        UUID tenantId = subscription.getTenantId();

        // Get tenant owners
        List<TenantOwnerEntity> owners = tenantOwnerRepository.findAllByTenantId(tenantId);

        for (TenantOwnerEntity owner : owners) {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("subscriptionId", subscription.getId());
            notificationData.put("previousPlan", subscription.getPlan());
            notificationData.put("newPlan", SubscriptionPlan.FREE);

//            adminNotificationService.sendSubscriptionExpiredNotification(
//                    owner.getUserId(), notificationData);
        }
    }

    /**
     * Notify about subscription cancellation
     */
    private void notifySubscriptionCancellation(SubscriptionEntity subscription) {
        UUID tenantId = subscription.getTenantId();

        // Get tenant owners
        List<TenantOwnerEntity> owners = tenantOwnerRepository.findAllByTenantId(tenantId);

        for (TenantOwnerEntity owner : owners) {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("subscriptionId", subscription.getId());
            notificationData.put("plan", subscription.getPlan());
            notificationData.put("cancellationReason", subscription.getCancellationReason());

//            adminNotificationService.sendSubscriptionCancelledNotification(
//                    owner.getUserId(), notificationData);
        }
    }
}