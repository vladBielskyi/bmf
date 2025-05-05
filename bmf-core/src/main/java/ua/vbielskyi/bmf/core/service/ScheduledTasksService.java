package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.bot.TelegramSessionEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.bot.TelegramSessionRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksService {

    private final TelegramSessionRepository telegramSessionRepository;
    private final TenantRepository tenantRepository;
    private final SalesAnalyticsService salesAnalyticsService;

    /**
     * Clean up old Telegram sessions (daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldSessions() {
        log.info("Starting scheduled cleanup of old telegram sessions");

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);

        List<TenantEntity> activeTenants = tenantRepository.findAllByActiveTrue();
        int totalRemoved = 0;

        for (TenantEntity tenant : activeTenants) {
            List<TelegramSessionEntity> oldSessions =
                    telegramSessionRepository.findAllByTenantIdAndLastActivityAtBefore(
                            tenant.getId(), cutoffTime);

            if (!oldSessions.isEmpty()) {
                telegramSessionRepository.deleteAllByTenantIdAndLastActivityAtBefore(
                        tenant.getId(), cutoffTime);
                totalRemoved += oldSessions.size();

                log.info("Removed {} old sessions for tenant: {}",
                        oldSessions.size(), tenant.getId());
            }
        }

        log.info("Completed cleanup of old telegram sessions. Total removed: {}", totalRemoved);
    }

    /**
     * Generate daily sales reports (every day at 1 AM)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailySalesReports() {
        log.info("Starting scheduled generation of daily sales reports");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<TenantEntity> activeTenants = tenantRepository.findAllByActiveTrue();
        int reportsGenerated = 0;

        for (TenantEntity tenant : activeTenants) {
            try {
                // Generate overall tenant report (no specific location)
                salesAnalyticsService.generateDailySalesReport(tenant.getId(), yesterday, null);
                reportsGenerated++;

                // TODO: Generate location-specific reports if needed

                log.info("Generated daily sales report for tenant: {}, date: {}",
                        tenant.getId(), yesterday);
            } catch (Exception e) {
                log.error("Error generating sales report for tenant: {}", tenant.getId(), e);
            }
        }

        log.info("Completed generation of daily sales reports. Total generated: {}", reportsGenerated);
    }

    /**
     * Check for subscription expirations (daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkSubscriptionExpirations() {
        log.info("Starting scheduled check of subscription expirations");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.plusDays(7); // 7 days before expiration

        List<TenantEntity> activeTenants = tenantRepository.findAllByActiveTrue();
        int expiringCount = 0;
        int expiredCount = 0;

        for (TenantEntity tenant : activeTenants) {
            if (tenant.getSubscriptionExpiryDate() != null) {
                if (tenant.getSubscriptionExpiryDate().isBefore(now)) {
                    // Subscription expired
                    tenant.setActive(false);
                    tenantRepository.save(tenant);
                    expiredCount++;

                    log.info("Deactivated tenant with expired subscription: {}", tenant.getId());

                    // TODO: Send notification to tenant owner
                } else if (tenant.getSubscriptionExpiryDate().isBefore(warningThreshold)) {
                    // Subscription expiring soon
                    expiringCount++;

                    log.info("Tenant subscription expiring soon: {}, expires: {}",
                            tenant.getId(), tenant.getSubscriptionExpiryDate());

                    // TODO: Send warning notification to tenant owner
                }
            }
        }

        log.info("Completed check of subscription expirations. " +
                "Expiring soon: {}, Expired and deactivated: {}", expiringCount, expiredCount);
    }
}