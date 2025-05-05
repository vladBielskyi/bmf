package ua.vbielskyi.bmf.api.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotRegistrationService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotStartupRegistrar {

    private final TenantRepository tenantRepository;
    private final BotRegistrationService botRegistrationService;

    @Value("${bot.admin.token}")
    private String adminBotToken;

    @Value("${bot.admin.username}")
    private String adminBotUsername;

    @Value("${bot.webhook.base-url}")
    private String webhookBaseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void registerBotsOnStartup() {
        log.info("Starting bot registration process on application startup");

        registerAdminBot();
        registerActiveTenantBots();
    }

    private void registerAdminBot() {
        log.info("Registering admin bot");

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
            log.error("Failed to register admin bot");
        }
    }

    private void registerActiveTenantBots() {
        log.info("Registering active tenant bots");

        List<TenantEntity> activeTenants = tenantRepository.findAllByActiveTrue();
        int successCount = 0;

        for (TenantEntity tenant : activeTenants) {
            String webhookUrl = webhookBaseUrl + "/webhook/tenant/" + tenant.getId();
            boolean success = botRegistrationService.registerBot(
                    BotType.TENANT,
                    tenant.getTelegramBotToken(),
                    tenant.getTelegramBotUsername(),
                    webhookUrl,
                    tenant.getId()
            );

            if (success) {
                successCount++;
                log.info("Successfully registered tenant bot: {}", tenant.getId());
            } else {
                log.error("Failed to register tenant bot: {}", tenant.getId());
            }
        }

        log.info("Successfully registered {}/{} active tenant bots", successCount, activeTenants.size());
    }
}