package ua.vbielskyi.bmf.api.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationRequest;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationResponse;
import ua.vbielskyi.bmf.core.tg.service.BotRegistrationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRegistrationApplicationService {

    private final BotRegistrationService botRegistrationService;

    @Value("${bot.webhook.admin-base-url}")
    private String adminWebhookBaseUrl;

    @Value("${bot.webhook.tenant-base-url}")
    private String tenantWebhookBaseUrl;

    /**
     * Register a bot
     */
    public BotRegistrationResponse registerBot(BotRegistrationRequest request) {
        try {
            String webhookUrl;

            if ("admin".equals(request.getBotType())) {
                // Admin bot
                webhookUrl = adminWebhookBaseUrl + "/webhook/admin";
                request.setTenantId(null);
            } else {
                // Tenant bot
                if (request.getTenantId() == null) {
                    return new BotRegistrationResponse(false, null, "Tenant ID is required for tenant bots");
                }
                webhookUrl = tenantWebhookBaseUrl + "/webhook/tenant/" + request.getTenantId();
            }

            boolean success = botRegistrationService.registerBot(
                    request.getToken(),
                    request.getUsername(),
                    webhookUrl,
                    request.getTenantId()
            );

            if (success) {
                return new BotRegistrationResponse(true, webhookUrl, null);
            } else {
                return new BotRegistrationResponse(false, null, "Failed to register bot");
            }
        } catch (Exception e) {
            log.error("Error registering bot", e);
            return new BotRegistrationResponse(false, null, e.getMessage());
        }
    }

    /**
     * Update webhook for a bot
     */
    public BotRegistrationResponse updateWebhook(BotRegistrationRequest request) {
        try {
            String webhookUrl;

            if ("admin".equals(request.getBotType())) {
                // Admin bot
                webhookUrl = adminWebhookBaseUrl + "/webhook/admin";
                request.setTenantId(null);
            } else {
                // Tenant bot
                if (request.getTenantId() == null) {
                    return new BotRegistrationResponse(false, null, "Tenant ID is required for tenant bots");
                }
                webhookUrl = tenantWebhookBaseUrl + "/webhook/tenant/" + request.getTenantId();
            }

            boolean success = botRegistrationService.updateWebhook(
                    request.getToken(),
                    webhookUrl,
                    request.getTenantId()
            );

            if (success) {
                return new BotRegistrationResponse(true, webhookUrl, null);
            } else {
                return new BotRegistrationResponse(false, null, "Failed to update webhook");
            }
        } catch (Exception e) {
            log.error("Error updating webhook", e);
            return new BotRegistrationResponse(false, null, e.getMessage());
        }
    }

    /**
     * Unregister a bot
     */
    public BotRegistrationResponse unregisterBot(BotRegistrationRequest request) {
        try {
            boolean success = botRegistrationService.unregisterBot(
                    request.getToken(),
                    request.getTenantId()
            );

            if (success) {
                return new BotRegistrationResponse(true, null, null);
            } else {
                return new BotRegistrationResponse(false, null, "Failed to unregister bot");
            }
        } catch (Exception e) {
            log.error("Error unregistering bot", e);
            return new BotRegistrationResponse(false, null, e.getMessage());
        }
    }
}
