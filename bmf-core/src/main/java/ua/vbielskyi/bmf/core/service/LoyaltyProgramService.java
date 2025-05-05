package ua.vbielskyi.bmf.core.service.loyalty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyProgramEntity;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyTierEntity;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyTransactionEntity;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyTransactionType;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.loyalty.LoyaltyProgramRepository;
import ua.vbielskyi.bmf.core.repository.loyalty.LoyaltyTierRepository;
import ua.vbielskyi.bmf.core.repository.loyalty.LoyaltyTransactionRepository;
import ua.vbielskyi.bmf.core.service.notification.NotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyProgramService {

    private final LoyaltyProgramRepository loyaltyProgramRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    /**
     * Create or update loyalty program for a tenant
     */
    @Transactional
    public LoyaltyProgramEntity createOrUpdateLoyaltyProgram(UUID tenantId, String name,
                                                             String description, boolean active,
                                                             BigDecimal pointsPerCurrency) {
        // Check if program already exists
        Optional<LoyaltyProgramEntity> existingProgram = loyaltyProgramRepository.findByTenantId(tenantId);

        LoyaltyProgramEntity program;
        if (existingProgram.isPresent()) {
            // Update existing program
            program = existingProgram.get();
            program.setName(name);
            program.setDescription(description);
            program.setActive(active);
            program.setPointsPerCurrency(pointsPerCurrency);
            program.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new program
            program = new LoyaltyProgramEntity();
            program.setTenantId(tenantId);
            program.setName(name);
            program.setDescription(description);
            program.setActive(active);
            program.setPointsPerCurrency(pointsPerCurrency);
            program.setCreatedAt(LocalDateTime.now());
            program.setUpdatedAt(LocalDateTime.now());
        }

        return loyaltyProgramRepository.save(program);
    }

    /**
     * Add a tier to a loyalty program
     */
    @Transactional
    public LoyaltyTierEntity addTier(UUID programId, String name, String description,
                                     int requiredPoints, BigDecimal discountPercentage,
                                     int bonusPointsMultiplier) {
        LoyaltyProgramEntity program = loyaltyProgramRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty program", "id", programId));

        LoyaltyTierEntity tier = new LoyaltyTierEntity();
        tier.setProgramId(programId);
        tier.setName(name);
        tier.setDescription(description);
        tier.setRequiredPoints(requiredPoints);
        tier.setDiscountPercentage(discountPercentage);
        tier.setBonusPointsMultiplier(bonusPointsMultiplier);
        tier.setCreatedAt(LocalDateTime.now());
        tier.setUpdatedAt(LocalDateTime.now());

        return loyaltyTierRepository.save(tier);
    }

    /**
     * Award points for a purchase
     */
    @Transactional
    public void awardPointsForPurchase(OrderEntity order) {
        UUID tenantId = order.getTenantId();
        UUID customerId = order.getCustomerId();

        // Check if loyalty program is active
        Optional<LoyaltyProgramEntity> programOpt = loyaltyProgramRepository.findByTenantId(tenantId);
        if (programOpt.isEmpty() || !programOpt.get().isActive()) {
            log.debug("No active loyalty program for tenant: {}", tenantId);
            return;
        }

        LoyaltyProgramEntity program = programOpt.get();

        // Get customer
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Calculate points to award
        BigDecimal orderAmount = order.getFinalAmount();
        BigDecimal pointsToAward = orderAmount.multiply(program.getPointsPerCurrency())
                .setScale(0, RoundingMode.DOWN);

        // Apply tier multiplier if applicable
        LoyaltyTierEntity customerTier = getCurrentTier(customer);
        if (customerTier != null) {
            int multiplier = customerTier.getBonusPointsMultiplier();
            if (multiplier > 1) {
                pointsToAward = pointsToAward.multiply(BigDecimal.valueOf(multiplier))
                        .setScale(0, RoundingMode.DOWN);
            }
        }

        // Create transaction
        LoyaltyTransactionEntity transaction = new LoyaltyTransactionEntity();
        transaction.setTenantId(tenantId);
        transaction.setCustomerId(customerId);
        transaction.setOrderId(order.getId());
        transaction.setPointsAmount(pointsToAward.intValue());
        transaction.setType(LoyaltyTransactionType.EARNED);
        transaction.setDescription("Points earned for order #" + order.getOrderNumber());
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        // Update customer loyalty points
        Integer currentPoints = customer.getLoyaltyPoints();
        if (currentPoints == null) {
            currentPoints = 0;
        }

        int newPoints = currentPoints + pointsToAward.intValue();
        customer.setLoyaltyPoints(newPoints);
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        // Check if customer has reached a new tier
        LoyaltyTierEntity newTier = getEligibleTier(program.getId(), newPoints);
        if (newTier != null && (customerTier == null ||
                newTier.getRequiredPoints() > customerTier.getRequiredPoints())) {
            // Customer reached a new tier
            sendTierUpgradeNotification(customer, newTier);
        }

        // Send points awarded notification
        sendPointsAwardedNotification(customer, pointsToAward.intValue(), order.getOrderNumber());

        log.info("Awarded {} loyalty points to customer {} for order {}",
                pointsToAward, customerId, order.getId());
    }

    /**
     * Redeem points for a discount
     */
    @Transactional
    public BigDecimal redeemPointsForDiscount(UUID tenantId, UUID customerId, int points) {
        // Check if loyalty program is active
        LoyaltyProgramEntity program = loyaltyProgramRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty program", "tenantId", tenantId));

        if (!program.isActive()) {
            throw new IllegalStateException("Loyalty program is not active");
        }

        // Get customer
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Check if customer has enough points
        Integer currentPoints = customer.getLoyaltyPoints();
        if (currentPoints == null || currentPoints < points) {
            throw new IllegalArgumentException("Customer does not have enough loyalty points");
        }

        // Calculate discount amount (1 point = 0.01 currency unit)
        BigDecimal discountAmount = BigDecimal.valueOf(points)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Create transaction
        LoyaltyTransactionEntity transaction = new LoyaltyTransactionEntity();
        transaction.setTenantId(tenantId);
        transaction.setCustomerId(customerId);
        transaction.setPointsAmount(-points); // Negative for redemption
        transaction.setType(LoyaltyTransactionType.REDEEMED);
        transaction.setDescription("Points redeemed for discount");
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        // Update customer loyalty points
        customer.setLoyaltyPoints(currentPoints - points);
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        // Send notification
        sendPointsRedeemedNotification(customer, points, discountAmount);

        log.info("Redeemed {} loyalty points for customer {} for a discount of {}",
                points, customerId, discountAmount);

        return discountAmount;
    }

    /**
     * Get customer's current tier
     */
    public LoyaltyTierEntity getCurrentTier(CustomerEntity customer) {
        Integer points = customer.getLoyaltyPoints();
        if (points == null || points == 0) {
            return null;
        }

        // Find all loyalty programs for this tenant
        Optional<LoyaltyProgramEntity> programOpt = loyaltyProgramRepository.findByTenantId(customer.getTenantId());
        if (programOpt.isEmpty()) {
            return null;
        }

        return getEligibleTier(programOpt.get().getId(), points);
    }

    /**
     * Get eligible tier for a given number of points
     */
    private LoyaltyTierEntity getEligibleTier(UUID programId, int points) {
        // Get all tiers for this program
        List<LoyaltyTierEntity> tiers = loyaltyTierRepository.findAllByProgramIdOrderByRequiredPointsDesc(programId);

        // Find highest tier the customer qualifies for
        for (LoyaltyTierEntity tier : tiers) {
            if (points >= tier.getRequiredPoints()) {
                return tier;
            }
        }

        return null;
    }

    /**
     * Send notification when customer reaches a new tier
     */
    private void sendTierUpgradeNotification(CustomerEntity customer, LoyaltyTierEntity tier) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customerName", customer.getFirstName());
        templateData.put("tierName", tier.getName());
        templateData.put("tierDescription", tier.getDescription());
        templateData.put("discountPercentage", tier.getDiscountPercentage());
        templateData.put("bonusPointsMultiplier", tier.getBonusPointsMultiplier());

        String message = String.format(
                "🎉 Congratulations, %s! You've reached the %s tier in our loyalty program!\n\n%s\n\n" +
                        "Benefits:\n- %s%% discount on orders\n- %sx bonus points on purchases",
                customer.getFirstName(),
                tier.getName(),
                tier.getDescription(),
                tier.getDiscountPercentage(),
                tier.getBonusPointsMultiplier()
        );

        notificationService.sendPromotionalNotification(
                customer.getTenantId(), customer.getTelegramId(),
                "New Loyalty Tier Reached!", message);
    }

    /**
     * Send notification when points are awarded
     */
    private void sendPointsAwardedNotification(CustomerEntity customer, int points, String orderNumber) {
        String message = String.format(
                "🌟 You earned %d loyalty points for your order #%s!\n\n" +
                        "Your current balance: %d points",
                points,
                orderNumber,
                customer.getLoyaltyPoints()
        );

        notificationService.sendPromotionalNotification(
                customer.getTenantId(), customer.getTelegramId(),
                "Loyalty Points Earned!", message);
    }

    /**
     * Send notification when points are redeemed
     */
    private void sendPointsRedeemedNotification(CustomerEntity customer, int points, BigDecimal discountAmount) {
        String message = String.format(
                "💰 You redeemed %d loyalty points for a discount of %s!\n\n" +
                        "Your current balance: %d points",
                points,
                discountAmount,
                customer.getLoyaltyPoints()
        );

        notificationService.sendPromotionalNotification(
                customer.getTenantId(), customer.getTelegramId(),
                "Loyalty Points Redeemed!", message);
    }

    /**
     * Get customer's loyalty status and point history
     */
    public Map<String, Object> getCustomerLoyaltyStatus(UUID tenantId, UUID customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        LoyaltyTierEntity currentTier = getCurrentTier(customer);

        List<LoyaltyTransactionEntity> transactions =
                transactionRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId);

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        result.put("points", customer.getLoyaltyPoints());
        result.put("currentTier", currentTier);
        result.put("transactions", transactions);

        // Calculate next tier if applicable
        if (currentTier != null) {
            List<LoyaltyTierEntity> allTiers = loyaltyTierRepository
                    .findAllByProgramIdOrderByRequiredPointsAsc(currentTier.getProgramId());

            LoyaltyTierEntity nextTier = null;
            for (LoyaltyTierEntity tier : allTiers) {
                if (tier.getRequiredPoints() > currentTier.getRequiredPoints()) {
                    nextTier = tier;
                    break;
                }
            }

            if (nextTier != null) {
                result.put("nextTier", nextTier);
                result.put("pointsToNextTier", nextTier.getRequiredPoints() - customer.getLoyaltyPoints());
            }
        }

        return result;
    }
}