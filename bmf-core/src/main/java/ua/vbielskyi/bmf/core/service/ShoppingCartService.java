package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.exception.ProductNotFoundException;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRATION = 7 * 24 * 60 * 60; // 7 days

    private final CacheService cacheService;
    private final ProductRepository productRepository;

    /**
     * Get cart for a customer
     */
    public Cart getCart(UUID tenantId, Long telegramId) {
        String cartKey = getCartKey(tenantId, telegramId);

        return cacheService.getOrElseCompute(
                cartKey,
                Cart.class,
                () -> new Cart(tenantId, telegramId),
                CART_EXPIRATION,
                TimeUnit.SECONDS,
                tenantId);
    }

    /**
     * Add product to cart
     */
    public Cart addToCart(UUID tenantId, Long telegramId, UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Get product details
        ProductEntity product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not active");
        }

        // Check available stock
        if (product.getAvailableStock() != null && product.getAvailableStock() < quantity) {
            throw new IllegalArgumentException("Not enough stock available");
        }

        // Get current cart
        Cart cart = getCart(tenantId, telegramId);

        // Create cart item
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setQuantity(quantity);

        // Set price (use discount price if available)
        BigDecimal price = product.getDiscountPrice() != null &&
                product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0 ?
                product.getDiscountPrice() : product.getPrice();
        item.setPrice(price);

        // Set image
        item.setImageUrl(product.getMainImageUrl());

        // Add to cart
        cart.addItem(item);

        // Save cart
        saveCart(cart);

        return cart;
    }

    /**
     * Update cart item quantity
     */
    public Cart updateCartItemQuantity(UUID tenantId, Long telegramId, UUID productId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        // Get current cart
        Cart cart = getCart(tenantId, telegramId);

        if (quantity == 0) {
            // Remove item from cart
            cart.removeItem(productId);
        } else {
            // Update quantity
            cart.updateItemQuantity(productId, quantity);
        }

        // Save cart
        saveCart(cart);

        return cart;
    }

    /**
     * Clear cart
     */
    public void clearCart(UUID tenantId, Long telegramId) {
        Cart cart = getCart(tenantId, telegramId);
        cart.clear();
        saveCart(cart);
    }

    /**
     * Save cart
     */
    private void saveCart(Cart cart) {
        String cartKey = getCartKey(cart.getTenantId(), cart.getTelegramId());

        cacheService.put(
                cartKey,
                cart,
                CART_EXPIRATION,
                TimeUnit.SECONDS,
                cart.getTenantId());
    }

    /**
     * Get cart cache key
     */
    private String getCartKey(UUID tenantId, Long telegramId) {
        return CART_KEY_PREFIX + tenantId + ":" + telegramId;
    }

    /**
     * Cart model
     */
    @lombok.Data
    public static class Cart {
        private UUID tenantId;
        private Long telegramId;
        private Map<UUID, CartItem> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Cart() {
            this.items = new HashMap<>();
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }

        public Cart(UUID tenantId, Long telegramId) {
            this();
            this.tenantId = tenantId;
            this.telegramId = telegramId;
        }

        /**
         * Add item to cart
         */
        public void addItem(CartItem item) {
            CartItem existingItem = items.get(item.getProductId());

            if (existingItem != null) {
                // Update existing item quantity
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            } else {
                // Add new item
                items.put(item.getProductId(), item);
            }

            this.updatedAt = LocalDateTime.now();
        }

        /**
         * Remove item from cart
         */
        public void removeItem(UUID productId) {
            items.remove(productId);
            this.updatedAt = LocalDateTime.now();
        }

        /**
         * Update item quantity
         */
        public void updateItemQuantity(UUID productId, int quantity) {
            CartItem item = items.get(productId);

            if (item != null) {
                item.setQuantity(quantity);
                this.updatedAt = LocalDateTime.now();
            }
        }

        /**
         * Clear cart
         */
        public void clear() {
            items.clear();
            this.updatedAt = LocalDateTime.now();
        }

        /**
         * Get total items count
         */
        public int getTotalItemsCount() {
            return items.values().stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        }

        /**
         * Get total price
         */
        public BigDecimal getTotalPrice() {
            return items.values().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * Get items as list
         */
        public List<CartItem> getItemsList() {
            return new ArrayList<>(items.values());
        }
    }

    /**
     * Cart item model
     */
    @lombok.Data
    public static class CartItem {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
        private String imageUrl;

        /**
         * Get total price for this item
         */
        public BigDecimal getTotalPrice() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}