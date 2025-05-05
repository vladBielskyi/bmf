package ua.vbielskyi.bmf.core.service.campaign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.campaign.CampaignEntity;
import ua.vbielskyi.bmf.core.entity.campaign.CampaignProductEntity;
import ua.vbielskyi.bmf.core.entity.campaign.CampaignType;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.campaign.CampaignProductRepository;
import ua.vbielskyi.bmf.core.repository.campaign.CampaignRepository;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    /**
     * Create a new campaign
     */
    public CampaignEntity createCampaign(UUID tenantId, String name, String description,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         CampaignType type, BigDecimal discountPercentage) {
        // Validate dates
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        // Create campaign
        CampaignEntity campaign = new CampaignEntity();
        campaign.setTenantId(tenantId);
        campaign.setName(name);
        campaign.setDescription(description);
        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);
        campaign.setType(type);
        campaign.setDiscountPercentage(discountPercentage);
        campaign.setActive(true);
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setUpdatedAt(LocalDateTime.now());

        CampaignEntity savedCampaign = campaignRepository.save(campaign);

        log.info("Created new campaign: {}, tenant: {}, type: {}",
                savedCampaign.getId(), tenantId, type);

        return savedCampaign;
    }

    /**
     * Add products to a campaign
     */
    public void addProductsToCampaign(UUID campaignId, List<UUID> productIds) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));

        // Add products to campaign
        for (UUID productId : productIds) {
            // Check if product exists
            ProductEntity product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

            // Check if product belongs to the same tenant
            if (!product.getTenantId().equals(campaign.getTenantId())) {
                throw new IllegalArgumentException("Product does not belong to the same tenant");
            }

            // Check if product is already in the campaign
            boolean exists = campaignProductRepository.existsByCampaignIdAndProductId(campaignId, productId);

            if (!exists) {
                // Calculate discounted price
                BigDecimal originalPrice = product.getPrice();
                BigDecimal discountPercentage = campaign.getDiscountPercentage();
                BigDecimal discountAmount = originalPrice.multiply(discountPercentage)
                        .divide(BigDecimal.valueOf(100));
                BigDecimal discountedPrice = originalPrice.subtract(discountAmount);

                // Create campaign product
                CampaignProductEntity campaignProduct = new CampaignProductEntity();
                campaignProduct.setCampaignId(campaignId);
                campaignProduct.setProductId(productId);
                campaignProduct.setOriginalPrice(originalPrice);
                campaignProduct.setDiscountedPrice(discountedPrice);
                campaignProduct.setCreatedAt(LocalDateTime.now());

                campaignProductRepository.save(campaignProduct);

                // Update product discount price if campaign is active
                if (campaign.isActive() && isCurrentlyActive(campaign)) {
                    product.setDiscountPrice(discountedPrice);
                    product.setUpdatedAt(LocalDateTime.now());
                    productRepository.save(product);
                }
            }
        }

        log.info("Added {} products to campaign: {}", productIds.size(), campaignId);
    }

    /**
     * Activate campaign
     */
    public void activateCampaign(UUID campaignId) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));

        campaign.setActive(true);
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        // If campaign is currently active, update product prices
        if (isCurrentlyActive(campaign)) {
            applyDiscountsToProducts(campaignId);
        }

        log.info("Activated campaign: {}", campaignId);
    }

    /**
     * Deactivate campaign
     */
    public void deactivateCampaign(UUID campaignId) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));

        campaign.setActive(false);
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        // If campaign was active, remove discounts from products
        if (isCurrentlyActive(campaign)) {
            removeDiscountsFromProducts(campaignId);
        }

        log.info("Deactivated campaign: {}", campaignId);
    }

    /**
     * Apply discounts to products in campaign
     */
    private void applyDiscountsToProducts(UUID campaignId) {
        List<CampaignProductEntity> campaignProducts = campaignProductRepository.findAllByCampaignId(campaignId);

        for (CampaignProductEntity campaignProduct : campaignProducts) {
            ProductEntity product = productRepository.findById(campaignProduct.getProductId()).orElse(null);

            if (product != null) {
                product.setDiscountPrice(campaignProduct.getDiscountedPrice());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);

                log.debug("Applied discount to product: {}, campaign: {}",
                        product.getId(), campaignId);
            }
        }
    }

    /**
     * Remove discounts from products in campaign
     */
    private void removeDiscountsFromProducts(UUID campaignId) {
        List<CampaignProductEntity> campaignProducts = campaignProductRepository.findAllByCampaignId(campaignId);

        for (CampaignProductEntity campaignProduct : campaignProducts) {
            ProductEntity product = productRepository.findById(campaignProduct.getProductId()).orElse(null);

            if (product != null) {
                product.setDiscountPrice(null);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);

                log.debug("Removed discount from product: {}, campaign: {}",
                        product.getId(), campaignId);
            }
        }
    }

    /**
     * Notify customers about campaign
     */
    public void notifyCustomersAboutCampaign(UUID campaignId) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));

        UUID tenantId = campaign.getTenantId();

        // Get tenant
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        // Get active customers
        List<CustomerEntity> customers = customerRepository.findAllByTenantIdAndActiveTrue(tenantId);

        // Generate notification message
        String message = String.format("🌸 Special Offer at %s! 🌸\n\n%s\n\n%s\n\nValid from %s to %s. Don't miss out!",
                tenant.getShopName(),
                campaign.getName(),
                campaign.getDescription(),
                formatDate(campaign.getStartDate()),
                formatDate(campaign.getEndDate()));

        // Send notifications
        int sentCount = 0;
        for (CustomerEntity customer : customers) {
            try {
                boolean sent = notificationService.sendPromotionalNotification(
                        tenantId, customer.getTelegramId(), campaign.getName(), message);

                if (sent) {
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Error sending campaign notification to customer: {}", customer.getId(), e);
            }
        }

        log.info("Sent campaign notifications to {}/{} customers for campaign: {}",
                sentCount, customers.size(), campaignId);
    }

    /**
     * Format date for notifications
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate().toString();
    }

    /**
     * Check if campaign is currently active
     */
    private boolean isCurrentlyActive(CampaignEntity campaign) {
        LocalDateTime now = LocalDateTime.now();
        return campaign.isActive() &&
                now.isAfter(campaign.getStartDate()) &&
                now.isBefore(campaign.getEndDate());
    }

    /**
     * Schedule to check and activate/deactivate campaigns daily
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void updateCampaignStatuses() {
        log.info("Running scheduled campaign status update");

        LocalDateTime now = LocalDateTime.now();

        // Find campaigns that should be activated (start date is today or in the past, and end date is in the future)
        List<CampaignEntity> campaignsToActivate = campaignRepository
                .findByActiveAndStartDateBeforeAndEndDateAfter(true, now, now);

        // Find campaigns that should be deactivated (end date is today or in the past)
        List<CampaignEntity> campaignsToDeactivate = campaignRepository
                .findByActiveAndEndDateBefore(true, now);

        // Activate campaigns
        for (CampaignEntity campaign : campaignsToActivate) {
            applyDiscountsToProducts(campaign.getId());
            log.info("Auto-activated campaign: {}", campaign.getId());
        }

        // Deactivate campaigns
        for (CampaignEntity campaign : campaignsToDeactivate) {
            removeDiscountsFromProducts(campaign.getId());
            campaign.setActive(false);
            campaign.setUpdatedAt(now);
            campaignRepository.save(campaign);
            log.info("Auto-deactivated campaign: {}", campaign.getId());
        }
    }

    /**
     * Create predefined campaigns for common holidays and events
     */
    public List<CampaignEntity> createPredefinedCampaigns(UUID tenantId, int year) {
        List<CampaignEntity> createdCampaigns = new ArrayList<>();

        // Valentine's Day
        createdCampaigns.add(createCampaign(
                tenantId,
                "Valentine's Day Special",
                "Express your love with our beautiful Valentine's flowers!",
                LocalDateTime.of(year, Month.FEBRUARY, 1, 0, 0),
                LocalDateTime.of(year, Month.FEBRUARY, 14, 23, 59),
                CampaignType.HOLIDAY,
                BigDecimal.valueOf(15)
        ));

        // Mother's Day (second Sunday in May)
        LocalDate mothersDay = findSecondSundayInMay(year);
        createdCampaigns.add(createCampaign(
                tenantId,
                "Mother's Day Collection",
                "Show mom how much you care with our special Mother's Day flowers!",
                LocalDateTime.of(mothersDay.minusDays(14), 0, 0),
                LocalDateTime.of(mothersDay, 23, 59),
                CampaignType.HOLIDAY,
                BigDecimal.valueOf(10)
        ));

        // Summer Sale
        createdCampaigns.add(createCampaign(
                tenantId,
                "Summer Bloom Sale",
                "Brighten your summer with our colorful flower arrangements!",
                LocalDateTime.of(year, Month.JUNE, 1, 0, 0),
                LocalDateTime.of(year, Month.AUGUST, 31, 23, 59),
                CampaignType.SEASONAL,
                BigDecimal.valueOf(20)
        ));

        // Winter Holiday
        createdCampaigns.add(createCampaign(
                tenantId,
                "Winter Holiday Collection",
                "Celebrate the season with our festive flower arrangements!",
                LocalDateTime.of(year, Month.DECEMBER, 1, 0, 0),
                LocalDateTime.of(year, Month.DECEMBER, 31, 23, 59),
                CampaignType.HOLIDAY,
                BigDecimal.valueOf(15)
        ));

        return createdCampaigns;
    }

    /**
     * Find the second Sunday in May for Mother's Day
     */
    private LocalDate findSecondSundayInMay(int year) {
        LocalDate date = LocalDate.of(year, Month.MAY, 1);

        // Find first Sunday
        while (date.getDayOfWeek().getValue() != 7) {
            date = date.plusDays(1);
        }

        // Add 7 days to get second Sunday
        return date.plusDays(7);
    }
}