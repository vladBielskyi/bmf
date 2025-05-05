package ua.vbielskyi.bmf.api.telegram.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationRequest;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationResponse;
import ua.vbielskyi.bmf.core.tg.model.BotType;
import ua.vbielskyi.bmf.core.tg.service.BotRegistrationService;
import ua.vbielskyi.bmf.core.tg.service.impl.CachedBotRegistry;

@Slf4j
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotRegistrationController {

    private final BotRegistrationService botRegistrationService;
    private final CachedBotRegistry botRegistry;

    @Value("${bot.webhook.base-url}")
    private String webhookBaseUrl;

    /**
     * Register a new bot
     */
    @PostMapping
    public ResponseEntity<BotRegistrationResponse> registerBot(@RequestBody BotRegistrationRequest request) {
        BotType botType = request.getBotType();
        if (botType == null) {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + request.getBotType()));
        }

        UUID tenantId = request.getTenantId();
        String webhookUrl;

        if (botType.isAdmin()) {
            webhookUrl = webhookBaseUrl + "/webhook/admin";
            tenantId = null;
        } else if (botType.isTenant()) {
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        new BotRegistrationResponse(false, null, "Tenant ID is required for tenant bots"));
            }
            webhookUrl = webhookBaseUrl + "/webhook/tenant/" + tenantId;
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + botType));
        }

        // Register the bot with Telegram and store in cache
        boolean success = botRegistrationService.registerBot(
                botType,
                request.getToken(),
                request.getUsername(),
                webhookUrl,
                tenantId
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, webhookUrl, null));
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Failed to register bot with Telegram"));
        }
    }

    /**
     * Get all registered tenant bots
     */
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantBotSummaryResponse>> getAllTenantBots() {
        Map<UUID, CachedBotRegistry.BotConfig> tenants = botRegistry.getAllTenantBotConfigs();

        List<TenantBotSummaryResponse> response = new ArrayList<>();
        tenants.forEach((tenantId, config) -> {
            response.add(new TenantBotSummaryResponse(
                    tenantId,
                    config.getUsername(),
                    config.isActive(),
                    config.getWebhookUrl()
            ));
        });

        return ResponseEntity.ok(response);
    }

    /**
     * Update webhook for a bot
     */
    @PutMapping("/{botId}/webhook")
    public ResponseEntity<BotRegistrationResponse> updateWebhook(
            @PathVariable("botId") String botId,
            @RequestBody BotRegistrationRequest request) {

        BotType botType = BotType.fromValue(request.getBotType());
        if (botType == null) {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + request.getBotType()));
        }

        UUID tenantId = request.getTenantId();
        String webhookUrl;

        if (botType.isAdmin()) {
            webhookUrl = webhookBaseUrl + "/webhook/admin";
            tenantId = null;
        } else if (botType.isTenant()) {
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        new BotRegistrationResponse(false, null, "Tenant ID is required for tenant bots"));
            }

            // Verify tenant exists
            CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(botType, tenantId);
            if (config == null) {
                return ResponseEntity.notFound().build();
            }

            webhookUrl = webhookBaseUrl + "/webhook/tenant/" + tenantId;
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + botType));
        }

        // Update webhook with Telegram
        boolean success = botRegistrationService.updateWebhook(
                request.getToken(),
                webhookUrl,
                tenantId
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, webhookUrl, null));
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Failed to update webhook with Telegram"));
        }
    }

    /**
     * Activate/deactivate a tenant bot
     */
    @PutMapping("/tenant/{tenantId}/status")
    public ResponseEntity<BotRegistrationResponse> updateTenantStatus(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam("active") boolean active) {

        boolean success = botRegistry.updateBotActiveStatus(BotType.TENANT, tenantId, active);

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(
                    true,
                    null,
                    "Tenant status updated successfully"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Unregister a bot
     */
    @DeleteMapping("/{botId}")
    public ResponseEntity<BotRegistrationResponse> unregisterBot(
            @PathVariable("botId") String botId,
            @RequestBody BotRegistrationRequest request) {

        BotType botType = BotType.fromValue(request.getBotType());
        if (botType == null) {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + request.getBotType()));
        }

        UUID tenantId = request.getTenantId();

        if (botType.isAdmin()) {
            tenantId = null;
        } else if (botType.isTenant()) {
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        new BotRegistrationResponse(false, null, "Tenant ID is required for tenant bots"));
            }

            // Verify tenant exists
            CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(botType, tenantId);
            if (config == null) {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Invalid bot type: " + botType));
        }

        // Unregister the bot with Telegram
        boolean success = botRegistrationService.unregisterBot(
                request.getToken(),
                tenantId
        );

        if (success) {
            return ResponseEntity.ok(new BotRegistrationResponse(true, null, null));
        } else {
            return ResponseEntity.badRequest().body(
                    new BotRegistrationResponse(false, null, "Failed to unregister bot with Telegram"));
        }
    }
}