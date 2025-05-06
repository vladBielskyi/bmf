package ua.vbielskyi.bmf.tg.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.tenant.TenantOwnerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.service.TenantOnboardingService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final TenantOwnerRepository tenantOwnerRepository;
    private final TenantOnboardingService tenantOnboardingService;

    /**
     * Get all shops owned by a user
     */
    public List<TenantEntity> getUserShops(Long userId) {
        // Get tenant IDs for this user
        List<UUID> tenantIds = tenantOwnerRepository.findTenantIdsByUserId(userId);

        if (tenantIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get tenant details
        return tenantRepository.findAllById(tenantIds);
    }

    /**
     * Get a shop by ID
     */
    public TenantEntity getShopById(UUID shopId) {
        return tenantRepository.findById(shopId).orElse(null);
    }

    /**
     * Register a new shop
     */
    public TenantEntity registerShop(Long userId, String name, String shopName, String description,
                                     String botToken, String botUsername, String webhookBaseUrl) {
        try {
            // Create tenant with default FREE plan
            TenantEntity tenant = tenantOnboardingService.createTenant(
                    name, shopName, description, botToken, botUsername, userId, webhookBaseUrl
            );

            log.info("Created new shop: {}, owner: {}", tenant.getId(), userId);

            return tenant;
        } catch (Exception e) {
            log.error("Error creating shop", e);
            throw e;
        }
    }

    /**
     * Toggle shop bot activation status
     */
    public boolean toggleBotStatus(UUID shopId, boolean active) {
        try {
            // This uses the existing CachedBotRegistry to update the bot status
            return botRegistry.updateBotActiveStatus(BotType.TENANT, shopId, active);
        } catch (Exception e) {
            log.error("Error toggling bot status for shop: {}", shopId, e);
            return false;
        }
    }
}