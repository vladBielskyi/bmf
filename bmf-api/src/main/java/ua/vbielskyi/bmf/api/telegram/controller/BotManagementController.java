package ua.vbielskyi.bmf.api.telegram.controller;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.vbielskyi.bmf.core.tg.service.BotRegistrationService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotManagementController {

    private final BotRegistrationService botRegistrationService;

    @Value("${bot.webhook.admin-base-url}")
    private String adminWebhookBaseUrl;

    @Value("${bot.webhook.tenant-base-url}")
    private String tenantWebhookBaseUrl;

    /**
     * Register a new bot
     */
    @PostMapping
    public ResponseEntity<BotRegistrationResponse> registerBot(@RequestBody BotRegistrationRequest request) {
        UUID tenantId = request.getTenantId();
        String webhookUrl;

        if (tenantId == null) {
            // Admin bot
            webhookUrl = adminWebhookBaseUrl + "/webhook/admin";
        } else {
            // Tenant bot
            webhookUrl = tenantWebhookBaseUrl + "/webhook/tenant/" + tenantId;
        }

        boolean success = botRegistrationService.registerBot(
                request.getToken(),
                request.getUsername(),
                webhookUrl,
                tenantId
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, webhookUrl, null));
        } else {
            return ResponseEntity.badRequest().body(new BotRegistrationResponse(false, null, "Failed to register bot"));
        }
    }

    /**
     * Update webhook for a bot
     */
    @PutMapping("/{botId}/webhook")
    public ResponseEntity<BotRegistrationResponse> updateWebhook(
            @PathVariable("botId") String botId,
            @RequestBody BotRegistrationRequest request) {

        UUID tenantId = request.getTenantId();
        String webhookUrl;

        if (tenantId == null) {
            // Admin bot
            webhookUrl = adminWebhookBaseUrl + "/webhook/admin";
        } else {
            // Tenant bot
            webhookUrl = tenantWebhookBaseUrl + "/webhook/tenant/" + tenantId;
        }

        boolean success = botRegistrationService.updateWebhook(
                request.getToken(),
                webhookUrl,
                tenantId
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, webhookUrl, null));
        } else {
            return ResponseEntity.badRequest().body(new BotRegistrationResponse(false, null, "Failed to update webhook"));
        }
    }

    /**
     * Unregister a bot
     */
    @DeleteMapping("/{botId}")
    public ResponseEntity<BotRegistrationResponse> unregisterBot(
            @PathVariable("botId") String botId,
            @RequestBody BotRegistrationRequest request) {

        boolean success = botRegistrationService.unregisterBot(
                request.getToken(),
                request.getTenantId()
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, null, null));
        } else {
            return ResponseEntity.badRequest().body(new BotRegistrationResponse(false, null, "Failed to unregister bot"));
        }
    }
}
