package ua.vbielskyi.bmf.api.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotRegistrationService;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotStartupRegistrar {

    private final TenantRepository tenantRepository;
    private final BotRegistrationService botRegistrationService;
    private final CachedBotRegistry botRegistry;

    @Value("${bot.admin.token}")
    private String adminBotToken;

    @Value("${bot.admin.username}")
    private String adminBotUsername;

    @Value("${bot.webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${bot.registration.fail-fast:false}")
    private boolean failFast;

    @Value("${bot.registration.batch-size:10}")
    private int batchSize;

    @EventListener(ApplicationReadyEvent.class)
    public void registerBotsOnStartup() {
        log.info("Starting bot registration process on application startup");

        try {
            // Register admin bot first
            registerAdminBot();

            // Then register tenant bots in batches
            registerActiveTenantBots();

            log.info("Bot registration process completed successfully");
        } catch (Exception e) {
            String errorMessage = "Error during bot registration startup process";
            log.error(errorMessage, e);

            if (failFast) {
                throw new RuntimeException(errorMessage, e);
            }
        }
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    protected void registerAdminBot() {
        log.info("Registering admin bot");

        // Check if admin bot is already registered
        CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(BotType.ADMIN, null);
        if (config != null) {
            log.info("Admin bot already registered, skipping registration");
            return;
        }

        String webhookUrl = webhookBaseUrl + "/webhook/admin";
        boolean success = botRegistrationService.registerBot(
                BotType.ADMIN,
                adminBotToken,
                adminBotUsername,
                webhookUrl,
                null
        );

        if (success) {
            log.info("Successfully registered admin bot");
        } else {
            String errorMessage = "Failed to register admin bot";
            log.error(errorMessage);

            if (failFast) {
                throw new RuntimeException(errorMessage);
            }
        }
    }

    private void registerActiveTenantBots() {
        log.info("Registering active tenant bots");

        List<TenantEntity> activeTenants = tenantRepository.findAllByActiveTrue();
        if (activeTenants.isEmpty()) {
            log.info("No active tenants found to register bots for");
            return;
        }

        log.info("Found {} active tenant(s) to register bots for", activeTenants.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // Process tenants in batches to avoid overwhelming Telegram API
        for (int i = 0; i < activeTenants.size(); i += batchSize) {
            int end = Math.min(i + batchSize, activeTenants.size());
            List<TenantEntity> batch = activeTenants.subList(i, end);

            log.info("Processing tenant batch {}-{} of {}", i + 1, end, activeTenants.size());

            for (TenantEntity tenant : batch) {
                try {
                    processTenantRegistration(tenant, successCount, failureCount, skipCount);

                    // Add a small delay between registrations to avoid rate limiting
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Tenant bot registration process interrupted", e);
                    break;
                } catch (Exception e) {
                    log.error("Error processing tenant bot registration for tenant: {}", tenant.getId(), e);
                    failureCount.incrementAndGet();

                    if (failFast) {
                        throw new RuntimeException("Error processing tenant bot registration", e);
                    }
                }
            }
        }

        log.info("Tenant bot registration complete: {} successful, {} skipped, {} failed",
                successCount.get(), skipCount.get(), failureCount.get());
    }

    private void processTenantRegistration(TenantEntity tenant,
                                           AtomicInteger successCount,
                                           AtomicInteger failureCount,
                                           AtomicInteger skipCount) {
        // Check if bot is already registered
        CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(BotType.TENANT, tenant.getId());
        if (config != null) {
            log.info("Bot for tenant {} already registered, skipping", tenant.getId());
            skipCount.incrementAndGet();
            return;
        }

        String webhookUrl = webhookBaseUrl + "/webhook/tenant/" + tenant.getId();

        try {
            boolean success = botRegistrationService.registerBot(
                    BotType.TENANT,
                    tenant.getTelegramBotToken(),
                    tenant.getTelegramBotUsername(),
                    webhookUrl,
                    tenant.getId()
            );

            if (success) {
                successCount.incrementAndGet();
                log.info("Successfully registered tenant bot: {}", tenant.getId());
            } else {
                failureCount.incrementAndGet();
                log.error("Failed to register tenant bot: {}", tenant.getId());
            }
        } catch (Exception e) {
            failureCount.incrementAndGet();
            log.error("Error registering tenant bot: {}", tenant.getId(), e);

            if (failFast) {
                throw e;
            }
        }
    }
}