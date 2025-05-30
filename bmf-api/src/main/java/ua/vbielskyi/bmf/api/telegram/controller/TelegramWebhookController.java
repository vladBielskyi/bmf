package ua.vbielskyi.bmf.api.telegram.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.telegram.handler.BotHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;

import java.util.UUID;

/**
 * Controller for handling Telegram webhooks
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final CachedBotRegistry botRegistry;

    /**
     * Handle updates for the admin bot
     */
    @PostMapping("/admin")
    public ResponseEntity<BotApiMethod<?>> handleAdminUpdate(@RequestBody Update update) {
        log.debug("Received update for admin bot: {}", update.getUpdateId());

            CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(BotType.ADMIN, null);
        if (config == null || !config.isActive()) {
            log.error("Admin bot not registered or inactive");
            return ResponseEntity.notFound().build();
        }

        BotHandler handler = botRegistry.findHandler(BotType.ADMIN, null);
        if (handler == null) {
            log.error("No handler found for admin bot");
            return ResponseEntity.notFound().build();
        }

        BotApiMethod<?> response = handler.handleUpdate(update, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle updates for tenant bots
     */
    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<BotApiMethod<?>> handleTenantUpdate(
            @PathVariable("tenantId") UUID tenantId,
            @RequestBody Update update) {

        log.debug("Received update for tenant {}: {}", tenantId, update.getUpdateId());

        CachedBotRegistry.BotConfig config = botRegistry.getBotConfig(BotType.TENANT, tenantId);
        if (config == null) {
            log.error("No tenant bot configuration found for: {}", tenantId);
            return ResponseEntity.notFound().build();
        }

        if (!config.isActive()) {
            log.warn("Received update for inactive tenant bot: {}", tenantId);
            return ResponseEntity.ok(null); // Silently acknowledge but don't process
        }

        BotHandler handler = botRegistry.findHandler(BotType.TENANT, tenantId);
        if (handler == null) {
            log.error("No handler found for tenant: {}", tenantId);
            return ResponseEntity.notFound().build();
        }

        BotApiMethod<?> response = handler.handleUpdate(update, tenantId);
        return ResponseEntity.ok(response);
    }
}