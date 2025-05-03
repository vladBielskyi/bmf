package ua.vbielskyi.bmf.tg.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.tenant.Tenant;
import ua.vbielskyi.bmf.core.event.EventPublisher;
import ua.vbielskyi.bmf.tg.admin.model.ShopSummary;
import ua.vbielskyi.bmf.tg.admin.repository.TenantOwnerRepository;
import ua.vbielskyi.bmf.tg.admin.repository.TenantRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tenant management operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantOwnerRepository tenantOwnerRepository;
    private final EventPublisher eventPublisher;

    /**
     * Check if a bot token is already in use
     *
     * @param token Bot token to check
     * @return true if token is already in use
     */
    public boolean isBotTokenInUse(String token) {
        return tenantRepository.existsByTelegramBotToken(token);
    }

    /**
     * Get all shops owned by a user
     *
     * @param userId User ID
     * @return List of tenant summaries
     */
    public List<ShopSummary> getShopsByUserId(Long userId) {
        List<UUID> tenantIds = tenantOwnerRepository.findTenantIdsByUserId(userId);

        return tenantRepository.findAllById(tenantIds).stream()
                .map(this::mapToShopSummary)
                .collect(Collectors.toList());
    }

    /**
     * Save a new tenant and associate it with an owner
     *
     * @param tenant Tenant to save
     * @param userId User ID of the owner
     * @return Saved tenant
     */
    @Transactional
    public Tenant saveTenant(Tenant tenant, Long userId) {
        // Save tenant
        Tenant savedTenant = tenantRepository.save(tenant);

        // Associate tenant with user
        tenantOwnerRepository.addOwnership(userId, savedTenant.getId());

        // Publish event for tenant creation
        eventPublisher.publishTenantCreated(savedTenant.getId(), savedTenant.getName());

        log.info("Created new tenant {} for user {}", savedTenant.getId(), userId);

        return savedTenant;
    }

    /**
     * Get a tenant by ID
     *
     * @param tenantId Tenant ID
     * @return Optional containing the tenant if found
     */
    public Optional<Tenant> getTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId);
    }

    /**
     * Activate a tenant
     *
     * @param tenantId Tenant ID
     * @return true if activation was successful
     */
    @Transactional
    public boolean activateTenant(UUID tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);

        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            tenant.setActive(true);
            tenantRepository.save(tenant);

            // Publish event
            eventPublisher.publishTenantActivated(tenantId);

            log.info("Activated tenant {}", tenantId);
            return true;
        }

        return false;
    }

    /**
     * Deactivate a tenant
     *
     * @param tenantId Tenant ID
     * @return true if deactivation was successful
     */
    @Transactional
    public boolean deactivateTenant(UUID tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);

        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            tenant.setActive(false);
            tenantRepository.save(tenant);

            // Publish event
            eventPublisher.publishTenantDeactivated(tenantId);

            log.info("Deactivated tenant {}", tenantId);
            return true;
        }

        return false;
    }

    /**
     * Update a tenant
     *
     * @param tenant Updated tenant data
     * @return Updated tenant
     */
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        // Verify tenant exists
        if (!tenantRepository.existsById(tenant.getId())) {
            throw new IllegalArgumentException("Tenant not found: " + tenant.getId());
        }

        Tenant savedTenant = tenantRepository.save(tenant);

        // Publish event
        eventPublisher.publishTenantUpdated(tenant.getId());

        log.info("Updated tenant {}", tenant.getId());

        return savedTenant;
    }

    /**
     * Check if a user is an owner of a tenant
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return true if user is an owner
     */
    public boolean isUserTenantOwner(Long userId, UUID tenantId) {
        return tenantOwnerRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Map a Tenant entity to a ShopSummary DTO
     *
     * @param tenant Tenant entity
     * @return ShopSummary DTO
     */
    private ShopSummary mapToShopSummary(Tenant tenant) {
        return ShopSummary.builder()
                .id(tenant.getId())
                .name(tenant.getShopName())
                .description(tenant.getDescription())
                .botUsername(tenant.getTelegramBotUsername())
                .active(tenant.isActive())
                .subscriptionPlan(tenant.getSubscriptionPlan().name())
                .subscriptionExpiryDate(tenant.getSubscriptionExpiryDate())
                .build();
    }
}