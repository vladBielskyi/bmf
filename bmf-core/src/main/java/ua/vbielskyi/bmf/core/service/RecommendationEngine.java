package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.analytics.CustomerAnalyticsEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.repository.analytics.CustomerAnalyticsRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderItemRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final CustomerAnalyticsRepository customerAnalyticsRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /**
     * Get personalized recommendations for a customer
     */
    public List<ProductEntity> getPersonalizedRecommendations(UUID tenantId, Long telegramId, int limit) {
        // Get recent purchases for this customer
        List<OrderEntity> customerOrders = orderRepository
                .findAllByTenantIdAndCustomerTelegramIdAndDeletedFalse(tenantId, telegramId);

        if (customerOrders.isEmpty()) {
            // New customer, return popular products instead
            return getPopularProducts(tenantId, limit);
        }

        // Get customer analytics
//        Optional<CustomerAnalyticsEntity> analytics = customerAnalyticsRepository
//                .findByTenantIdAndCustomerId(tenantId, customerOrders.get(0).getCustomerId());
//
//        // If we have analytics with favorite category, use it
//        if (analytics.isPresent() && analytics.get().getFavoriteCategoryId() != null) {
//            return getRecommendationsByCategory(tenantId, analytics.get().getFavoriteCategoryId(),
//                    getRecentlyPurchasedProductIds(customerOrders), limit);
//        }

        // Otherwise use collaborative filtering approach
        return getCollaborativeFilteringRecommendations(tenantId, customerOrders, limit);
    }

    /**
     * Get seasonal recommendations based on current time of year
     */
    public List<ProductEntity> getSeasonalRecommendations(UUID tenantId, int limit) {
        int month = LocalDateTime.now().getMonthValue();

        // Determine season based on month
        String season;
        if (month >= 3 && month <= 5) {
            season = "Spring";
        } else if (month >= 6 && month <= 8) {
            season = "Summer";
        } else if (month >= 9 && month <= 11) {
            season = "Autumn";
        } else {
            season = "Winter";
        }

        // Get products with the season tag
        List<ProductEntity> seasonalProducts = productRepository
                .findAllByTenantIdAndTagNameAndActiveTrue(tenantId, season);

        if (seasonalProducts.size() >= limit) {
            return seasonalProducts.subList(0, limit);
        }

        // If not enough seasonal products, add popular products
        List<ProductEntity> popularProducts = getPopularProducts(tenantId, limit - seasonalProducts.size());
        List<ProductEntity> result = new ArrayList<>(seasonalProducts);
        result.addAll(popularProducts);

        return result;
    }

    /**
     * Get occasion-based recommendations (birthday, anniversary, etc.)
     */
    public List<ProductEntity> getOccasionRecommendations(UUID tenantId, String occasion, int limit) {
        // Get products with the occasion tag
        List<ProductEntity> occasionProducts = productRepository
                .findAllByTenantIdAndTagNameAndActiveTrue(tenantId, occasion);

        if (occasionProducts.size() >= limit) {
            return occasionProducts.subList(0, limit);
        }

        // If not enough occasion products, add featured products
        List<ProductEntity> featuredProducts = productRepository
                .findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId);

        // Filter out products already in occasion products
        Set<UUID> occasionProductIds = occasionProducts.stream()
                .map(ProductEntity::getId)
                .collect(Collectors.toSet());

        List<ProductEntity> additionalProducts = featuredProducts.stream()
                .filter(p -> !occasionProductIds.contains(p.getId()))
                .limit(limit - occasionProducts.size())
                .collect(Collectors.toList());

        List<ProductEntity> result = new ArrayList<>(occasionProducts);
        result.addAll(additionalProducts);

        return result;
    }

    /**
     * Get popular products based on order history
     */
    public List<ProductEntity> getPopularProducts(UUID tenantId, int limit) {
        // Get recent orders
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<OrderEntity> recentOrders = orderRepository
                .findAllByTenantIdAndDateRangeAndDeletedFalse(tenantId, oneMonthAgo, LocalDateTime.now());

        if (recentOrders.isEmpty()) {
            // No recent orders, return featured products
            return productRepository.findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // Count product occurrences in orders
        Map<UUID, Integer> productCounts = new HashMap<>();

        for (OrderEntity order : recentOrders) {
            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                productCounts.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        // Sort products by popularity
        List<Map.Entry<UUID, Integer>> sortedProducts = productCounts.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Get product entities
        List<ProductEntity> result = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : sortedProducts) {
            ProductEntity product = productRepository.findById(entry.getKey()).orElse(null);

            if (product != null && product.isActive()) {
                result.add(product);
            }
        }

        // If not enough products, add featured products
        if (result.size() < limit) {
            Set<UUID> existingProductIds = result.stream()
                    .map(ProductEntity::getId)
                    .collect(Collectors.toSet());

            List<ProductEntity> featuredProducts = productRepository
                    .findAllByTenantIdAndFeaturedTrueAndActiveTrue(tenantId)
                    .stream()
                    .filter(p -> !existingProductIds.contains(p.getId()))
                    .limit(limit - result.size())
                    .collect(Collectors.toList());

            result.addAll(featuredProducts);
        }

        return result;
    }

    /**
     * Get recommendations by category, excluding already purchased products
     */
    private List<ProductEntity> getRecommendationsByCategory(UUID tenantId, UUID categoryId,
                                                             Set<UUID> purchasedProductIds, int limit) {
        List<ProductEntity> categoryProducts = productRepository
                .findAllByTenantIdAndCategoryIdAndActiveTrue(tenantId, categoryId);

        // Filter out already purchased products
        List<ProductEntity> filteredProducts = categoryProducts.stream()
                .filter(p -> !purchasedProductIds.contains(p.getId()))
                .limit(limit)
                .collect(Collectors.toList());

        // If not enough products, add recommendations from other categories
        if (filteredProducts.size() < limit) {
            List<ProductEntity> otherProducts = productRepository
                    .findAllByTenantIdAndActiveTrue(tenantId)
                    .stream()
                    .filter(p -> !p.getCategoryId().equals(categoryId) &&
                            !purchasedProductIds.contains(p.getId()))
                    .limit(limit - filteredProducts.size())
                    .collect(Collectors.toList());

            filteredProducts.addAll(otherProducts);
        }

        return filteredProducts;
    }

    /**
     * Get collaborative filtering recommendations based on similar customers
     */
    private List<ProductEntity> getCollaborativeFilteringRecommendations(UUID tenantId,
                                                                         List<OrderEntity> customerOrders,
                                                                         int limit) {
        // Get products purchased by this customer
        Set<UUID> purchasedProductIds = getRecentlyPurchasedProductIds(customerOrders);

        // Find customers who purchased the same products
        List<UUID> similarCustomerIds = findSimilarCustomers(tenantId, purchasedProductIds);

        // Get products these similar customers also purchased
        Set<UUID> recommendedProductIds = new HashSet<>();

        for (UUID customerId : similarCustomerIds) {
            // Get orders for similar customer
            List<OrderEntity> similarCustomerOrders = orderRepository
                    .findAllByTenantIdAndCustomerIdAndDeletedFalse(tenantId, customerId);

            // Collect their purchased products
            for (OrderEntity order : similarCustomerOrders) {
                List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());

                for (OrderItemEntity item : items) {
                    // Add products that the original customer hasn't purchased
                    if (!purchasedProductIds.contains(item.getProductId())) {
                        recommendedProductIds.add(item.getProductId());
                    }
                }
            }
        }

        // Get product entities
        List<ProductEntity> recommendations = new ArrayList<>();

        for (UUID productId : recommendedProductIds) {
            if (recommendations.size() >= limit) {
                break;
            }

            ProductEntity product = productRepository.findById(productId).orElse(null);

            if (product != null && product.isActive()) {
                recommendations.add(product);
            }
        }

        // If not enough recommendations, add popular products
        if (recommendations.size() < limit) {
            Set<UUID> existingRecommendationIds = recommendations.stream()
                    .map(ProductEntity::getId)
                    .collect(Collectors.toSet());

            List<ProductEntity> popularProducts = getPopularProducts(tenantId, limit)
                    .stream()
                    .filter(p -> !purchasedProductIds.contains(p.getId()) &&
                            !existingRecommendationIds.contains(p.getId()))
                    .limit(limit - recommendations.size())
                    .collect(Collectors.toList());

            recommendations.addAll(popularProducts);
        }

        return recommendations;
    }

    /**
     * Get recently purchased product IDs for a customer
     */
    private Set<UUID> getRecentlyPurchasedProductIds(List<OrderEntity> customerOrders) {
        Set<UUID> purchasedProductIds = new HashSet<>();

        for (OrderEntity order : customerOrders) {
            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                purchasedProductIds.add(item.getProductId());
            }
        }

        return purchasedProductIds;
    }

    /**
     * Find customers who have purchased similar products
     */
    private List<UUID> findSimilarCustomers(UUID tenantId, Set<UUID> purchasedProductIds) {
        // Find all orders with these products
        List<UUID> similarCustomerIds = new ArrayList<>();

        for (UUID productId : purchasedProductIds) {
            // Find order items for this product
//            List<OrderItemEntity> items = orderItemRepository.findAllByProductId(productId);
//
//            // Get order IDs
//            List<UUID> orderIds = items.stream()
//                    .map(OrderItemEntity::getOrderId)
//                    .collect(Collectors.toList());
//
//            // Get orders from this tenant
//            List<OrderEntity> orders = orderRepository.findAllById(orderIds)
//                    .stream()
//                    .filter(o -> o.getTenantId().equals(tenantId))
//                    .collect(Collectors.toList());
//
//            // Get customer IDs
//            for (OrderEntity order : orders) {
//                if (!similarCustomerIds.contains(order.getCustomerId())) {
//                    similarCustomerIds.add(order.getCustomerId());
//                }
//            }
        }

        return similarCustomerIds;
    }
}