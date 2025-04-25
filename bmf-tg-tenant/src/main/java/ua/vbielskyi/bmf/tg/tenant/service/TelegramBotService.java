package ua.vbielskyi.bmf.tg.tenant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ua.vbielskyi.bmf.common.model.tenant.Tenant;
import ua.vbielskyi.bmf.tg.tenant.bot.FlowerShopBot;
import ua.vbielskyi.bmf.tg.tenant.repository.TenantRepository;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for managing Telegram bots for each tenant
 */
@Service
@Slf4j
public class TelegramBotService {

    private final Map<UUID, FlowerShopBot> activeBots = new ConcurrentHashMap<>();
    private final TenantRepository tenantRepository;
    private final ApplicationContext applicationContext;
    private TelegramBotsApi telegramBotsApi;

    @Autowired
    public TelegramBotService(TenantRepository tenantRepository, ApplicationContext applicationContext) {
        this.tenantRepository = tenantRepository;
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize the bots when the application starts
     */
    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            registerAllActiveTenantBots();
        } catch (TelegramApiException e) {
            log.error("Failed to initialize Telegram bots API", e);
        }
    }

    /**
     * Register a new bot for a tenant
     */
    public void registerBot(Tenant tenant) {
        if (!tenant.isActive()) {
            log.warn("Attempted to register bot for inactive tenant: {}", tenant.getId());
            return;
        }

        // Check if bot is already registered
        if (activeBots.containsKey(tenant.getId())) {
            log.info("Bot for tenant {} is already registered", tenant.getId());
            return;
        }

        try {
            FlowerShopBot bot = new FlowerShopBot(
                    tenant.getId(),
                    tenant.getTelegramBotToken(),
                    tenant.getTelegramBotUsername(),
                    applicationContext
            );

            telegramBotsApi.registerBot(bot);
            activeBots.put(tenant.getId(), bot);

            log.info("Successfully registered bot for tenant: {}", tenant.getId());
        } catch (TelegramApiException e) {
            log.error("Failed to register bot for tenant: {}", tenant.getId(), e);
        }
    }

    /**
     * Unregister a bot for a tenant
     */
    public void unregisterBot(UUID tenantId) {
        FlowerShopBot bot = activeBots.get(tenantId);

        if (bot != null) {
            bot.onClosing();
            activeBots.remove(tenantId);
            log.info("Unregistered bot for tenant: {}", tenantId);
        }
    }

    /**
     * Register bots for all active tenants
     */
    private void registerAllActiveTenantBots() {
        List<Tenant> activeTenants = tenantRepository.findAllByActiveTrue();

        for (Tenant tenant : activeTenants) {
            registerBot(tenant);
        }

        log.info("Registered {} active tenant bots", activeTenants.size());
    }

    /**
     * Update bot configuration when tenant details change
     */
    public void updateBot(Tenant tenant) {
        // First unregister the existing bot
        unregisterBot(tenant.getId());

        // Then register with the new details
        if (tenant.isActive()) {
            registerBot(tenant);
        }
    }

    /**
     * Cleanup when application shuts down
     */
    @PreDestroy
    public void destroy() {
        for (Map.Entry<UUID, FlowerShopBot> entry : activeBots.entrySet()) {
            entry.getValue().onClosing();
            log.info("Shutdown bot for tenant: {}", entry.getKey());
        }
        activeBots.clear();
    }
}