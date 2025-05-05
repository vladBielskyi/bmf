package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.analytics.CustomerAnalyticsEntity;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.analytics.CustomerAnalyticsRepository;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderItemRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerAnalyticsRepository customerAnalyticsRepository;

    /**
     * Update customer profile analytics based on purchase history
     */
    public CustomerAnalyticsEntity updateCustomerAnalytics(UUID customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        UUID tenantId = customer.getTenantId();

        // Find customer orders
        List<OrderEntity> orders = orderRepository.findAllByTenantIdAndCustomerIdAndDeletedFalse(tenantId, customerId);

        if (orders.isEmpty()) {
            log.info("No orders found for customer: {}", customerId);
            return null;
        }

        // Calculate metrics
        int orderCount = orders.size();
        BigDecimal totalSpent = orders.stream()
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalSpent.divide(
                BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        LocalDateTime firstPurchaseDate = orders.stream()
                .map(OrderEntity::getCreatedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDateTime lastPurchaseDate = orders.stream()
                .map(OrderEntity::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // Find favorite category
        Map<UUID, Integer> categoryCounts = new HashMap<>();

        orders.forEach(order -> {
            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());
            items.forEach(item -> {
                ProductEntity product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null && product.getCategoryId() != null) {
                    categoryCounts.merge(product.getCategoryId(), item.getQuantity(), Integer::sum);
                }
            });
        });

        UUID favoriteCategoryId = null;
        String favoriteCategoryName = null;

        if (!categoryCounts.isEmpty()) {
            favoriteCategoryId = Collections.max(
                    categoryCounts.entrySet(),
                    Map.Entry.comparingByValue()
            ).getKey();

            // Get category name (would normally come from a category repository)
            // favoriteCategoryName = categoryRepository.findById(favoriteCategoryId)
            //        .map(CategoryEntity::getName)
            //        .orElse("Unknown");
        }

        // Save or update analytics
        CustomerAnalyticsEntity analytics = customerAnalyticsRepository
                .findById(customerId)
                .orElse(new CustomerAnalyticsEntity());

        analytics.setId(UUID.randomUUID());
        analytics.setTenantId(tenantId);
        analytics.setCustomerId(customerId);
        analytics.setOrderCount(orderCount);
        analytics.setTotalSpent(totalSpent);
        analytics.setAverageOrderValue(averageOrderValue);
        analytics.setFirstPurchaseDate(firstPurchaseDate);
        analytics.setLastPurchaseDate(lastPurchaseDate);
        analytics.setFavoriteCategoryId(favoriteCategoryId);
        analytics.setFavoriteCategoryName(favoriteCategoryName);
        analytics.setLastUpdatedAt(LocalDateTime.now());

        return customerAnalyticsRepository.save(analytics);
    }

    /**
     * Get personalized product recommendations for a customer
     */
    public List<UUID> getRecommendedProducts(UUID customerId, int limit) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        UUID tenantId = customer.getTenantId();

        // Get customer's previous purchases
        Set<UUID> purchasedProductIds = new HashSet<>();
        List<OrderEntity> orders = orderRepository.findAllByTenantIdAndCustomerIdAndDeletedFalse(tenantId, customerId);

        orders.forEach(order -> {
            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());
            items.forEach(item -> purchasedProductIds.add(item.getProductId()));
        });

        if (purchasedProductIds.isEmpty()) {
            // No purchase history, recommend featured products
            return productRepository.findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId)
                    .stream()
                    .map(ProductEntity::getId)
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // Find favorite category
        CustomerAnalyticsEntity analytics = customerAnalyticsRepository.findById(customerId).orElse(null);

        if (analytics != null && analytics.getFavoriteCategoryId() != null) {
            // Recommend products from favorite category that customer hasn't purchased yet
            List<UUID> recommendationsFromFavoriteCategory = productRepository
                    .findAllByTenantIdAndCategoryIdAndActiveTrue(tenantId, analytics.getFavoriteCategoryId())
                    .stream()
                    .filter(p -> !purchasedProductIds.contains(p.getId()))
                    .map(ProductEntity::getId)
                    .limit(limit)
                    .collect(Collectors.toList());

            if (recommendationsFromFavoriteCategory.size() >= limit) {
                return recommendationsFromFavoriteCategory;
            }

            // If not enough recommendations from favorite category, add more featured products
            List<UUID> additionalRecommendations = productRepository
                    .findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId)
                    .stream()
                    .filter(p -> !purchasedProductIds.contains(p.getId()) &&
                            !recommendationsFromFavoriteCategory.contains(p.getId()))
                    .map(ProductEntity::getId)
                    .limit(limit - recommendationsFromFavoriteCategory.size())
                    .collect(Collectors.toList());

            recommendationsFromFavoriteCategory.addAll(additionalRecommendations);
            return recommendationsFromFavoriteCategory;
        }

        // Fallback: recommend featured products the customer hasn't purchased yet
        return productRepository.findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId)
                .stream()
                .filter(p -> !purchasedProductIds.contains(p.getId()))
                .map(ProductEntity::getId)
                .limit(limit)
                .collect(Collectors.toList());
    }
}