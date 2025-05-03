package ua.vbielskyi.bmf.api.telegram.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.vbielskyi.bmf.core.tg.BotRegistry;
import ua.vbielskyi.bmf.core.tg.service.BotProcessorService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final BotRegistry botRegistry;

    /**
     * Handle updates for the admin bot
     */
    @PostMapping("/admin")
    public ResponseEntity<BotApiMethod<?>> handleAdminUpdate(@RequestBody Update update) {
        log.debug("Received update for admin bot: {}", update.getUpdateId());

        BotProcessorService processor = botRegistry.findProcessor("admin", null);

        if (processor == null) {
            log.error("No processor found for admin bot");
            return ResponseEntity.notFound().build();
        }

        BotApiMethod<?> response = processor.processUpdate(update, null);
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

        BotProcessorService processor = botRegistry.findProcessor("tenant", tenantId);

        if (processor == null) {
            log.error("No processor found for tenant: {}", tenantId);
            return ResponseEntity.notFound().build();
        }

        BotApiMethod<?> response = processor.processUpdate(update, tenantId);
        return ResponseEntity.ok(response);
    }
}
