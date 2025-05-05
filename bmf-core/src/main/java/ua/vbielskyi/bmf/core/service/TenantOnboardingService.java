package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.tenant.SubscriptionPlan;
import ua.vbielskyi.bmf.core.entity.bot.BotSettingsEntity;
import ua.vbielskyi.bmf.core.entity.bot.WebAppSettingsEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantLocationEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantOwnerEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantSettingsEntity;
import ua.vbielskyi.bmf.core.exception.DuplicateTenantException;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.bot.BotSettingsRepository;
import ua.vbielskyi.bmf.core.repository.bot.WebAppSettingsRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantLocationRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantOwnerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantSettingsRepository;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotRegistrationService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantOnboardingService {

    private final TenantRepository tenantRepository;
    private final TenantOwnerRepository tenantOwnerRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantLocationRepository tenantLocationRepository;
    private final BotSettingsRepository botSettingsRepository;
    private final WebAppSettingsRepository webAppSettingsRepository;
    private final BotRegistrationService botRegistrationService;

    /**
     * Create a new tenant with default settings
     */
    @Transactional
    public TenantEntity createTenant(String name, String shopName, String description,
                                     String telegramBotToken, String telegramBotUsername,
                                     Long ownerTelegramId, String webhookBaseUrl) {
        // Check if bot token is already in use
        if (tenantRepository.existsByTelegramBotToken(telegramBotToken)) {
            throw new DuplicateTenantException("Bot token is already in use");
        }

        // Check if bot username is already in use
        if (tenantRepository.existsByTelegramBotUsername(telegramBotUsername)) {
            throw new DuplicateTenantException("Bot username is already in use");
        }

        // Create tenant
        TenantEntity tenant = new TenantEntity();
        tenant.setName(name);
        tenant.setShopName(shopName);
        tenant.setDescription(description);
        tenant.setTelegramBotToken(telegramBotToken);
        tenant.setTelegramBotUsername(telegramBotUsername);
        tenant.setSubscriptionPlan(SubscriptionPlan.FREE);
        tenant.setActive(true);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        // Save tenant
        TenantEntity savedTenant = tenantRepository.save(tenant);
        UUID tenantId = savedTenant.getId();

        // Create tenant owner
        TenantOwnerEntity owner = new TenantOwnerEntity();
        owner.setTenantId(tenantId);
        owner.setUserId(ownerTelegramId);
        owner.setPrimary(true);
        owner.setCreatedAt(LocalDateTime.now());
        tenantOwnerRepository.save(owner);

        // Create default tenant settings
        createDefaultTenantSettings(tenantId);

        // Create default bot settings
        createDefaultBotSettings(tenantId);

        // Create default WebApp settings
        createDefaultWebAppSettings(tenantId);

        // Create default location
        createDefaultLocation(tenantId, shopName);

        // Register bot with Telegram
        String webhookUrl = webhookBaseUrl + "/webhook/tenant/" + tenantId;
        boolean success = botRegistrationService.registerBot(
                BotType.TENANT,
                telegramBotToken,
                telegramBotUsername,
                webhookUrl,
                tenantId
        );

        if (!success) {
            log.error("Failed to register bot with Telegram for tenant: {}", tenantId);
            // We continue anyway as the tenant is created and bot can be registered later
        }

        log.info("Created new tenant: {}, name: {}, owner: {}",
                tenantId, shopName, ownerTelegramId);

        return savedTenant;
    }

    /**
     * Create default tenant settings
     */
    private void createDefaultTenantSettings(UUID tenantId) {
        TenantSettingsEntity settings = new TenantSettingsEntity();
        settings.setTenantId(tenantId);
        settings.setDefaultLanguage("en");
        settings.setTimezone("UTC");
        settings.setCurrency("USD");
        settings.setMinOrderAmount(10.0);
        settings.setDeliveryFee(5.0);
        settings.setFreeDeliveryThreshold(50.0);
        settings.setEnablePayments(true);
        settings.setAllowCardPayments(true);
        settings.setAllowCashPayments(true);
        settings.setEnableLocationServices(true);
        settings.setEnableNotifications(true);
        settings.setAllowGuestCheckout(false);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());

        tenantSettingsRepository.save(settings);
    }

    /**
     * Create default bot settings
     */
    private void createDefaultBotSettings(UUID tenantId) {
        BotSettingsEntity settings = new BotSettingsEntity();
        settings.setTenantId(tenantId);
        settings.setWelcomeMessage("Welcome to our flower shop! How can I help you today?");
        settings.setOrderConfirmationMessage("Thank you for your order! We'll process it shortly.");
        settings.setDeliveryInstructions("Our courier will call you when they arrive.");
        settings.setDefaultLanguage("en");
        settings.setEnableLocationServices(true);
        settings.setEnablePayments(true);
        settings.setEnableWebApp(true);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());

        botSettingsRepository.save(settings);
    }

    /**
     * Create default WebApp settings
     */
    private void createDefaultWebAppSettings(UUID tenantId) {
        WebAppSettingsEntity settings = new WebAppSettingsEntity();
        settings.setTenantId(tenantId);
        settings.setPrimaryColor("#4CAF50");
        settings.setSecondaryColor("#2196F3");
        settings.setAccentColor("#FF9800");
        settings.setFontFamily("Arial, sans-serif");
        settings.setEnableCart(true);
        settings.setEnableFavorites(true);
        settings.setEnableSearch(true);
        settings.setEnableFilters(true);
        settings.setProductsPerPage(12);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());

        webAppSettingsRepository.save(settings);
    }

    /**
     * Create default location
     */
    private void createDefaultLocation(UUID tenantId, String shopName) {
        TenantLocationEntity location = new TenantLocationEntity();
        location.setTenantId(tenantId);
        location.setName("Main Store");
        location.setAddress("Please update with your store address");
        location.setActive(true);
        location.setCreatedAt(LocalDateTime.now());
        location.setUpdatedAt(LocalDateTime.now());

        tenantLocationRepository.save(location);
    }

    /**
     * Suspend tenant
     */
    @Transactional
    public void suspendTenant(UUID tenantId, String reason) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        tenant.setActive(false);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        log.info("Suspended tenant: {}, reason: {}", tenantId, reason);
    }

    /**
     * Activate tenant
     */
    @Transactional
    public void activateTenant(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        tenant.setActive(true);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        log.info("Activated tenant: {}", tenantId);
    }

    /**
     * Update tenant subscription plan
     */
    @Transactional
    public void updateSubscriptionPlan(UUID tenantId, SubscriptionPlan newPlan, int durationMonths) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        tenant.setSubscriptionPlan(newPlan);

        // Set or extend expiry date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiry = tenant.getSubscriptionExpiryDate();

        if (currentExpiry == null || currentExpiry.isBefore(now)) {
            // New subscription or expired
            tenant.setSubscriptionExpiryDate(now.plusMonths(durationMonths));
        } else {
            // Extend existing subscription
            tenant.setSubscriptionExpiryDate(currentExpiry.plusMonths(durationMonths));
        }

        tenant.setUpdatedAt(now);
        tenantRepository.save(tenant);

        log.info("Updated subscription plan for tenant: {} to {} for {} months",
                tenantId, newPlan, durationMonths);
    }
}