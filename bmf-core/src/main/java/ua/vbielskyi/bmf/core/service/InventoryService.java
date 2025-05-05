package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductLocationAvailabilityEntity;
import ua.vbielskyi.bmf.core.exception.InsufficientStockException;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.product.ProductLocationAvailabilityRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductLocationAvailabilityRepository locationAvailabilityRepository;

    /**
     * Check if product is available at a specific location
     */
    public boolean isProductAvailableAtLocation(UUID productId, UUID locationId, int quantity) {
        Optional<ProductLocationAvailabilityEntity> availability =
                locationAvailabilityRepository.findByProductIdAndLocationId(productId, locationId);

        return availability.isPresent() &&
                availability.get().isAvailable() &&
                availability.get().getStockQuantity() >= quantity;
    }

    /**
     * Reduce stock at a specific location
     */
    @Transactional
    public void reduceStockAtLocation(UUID productId, UUID locationId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductLocationAvailabilityEntity availability = locationAvailabilityRepository
                .findByProductIdAndLocationId(productId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product availability not found at location"));

        if (!availability.isAvailable()) {
            throw new IllegalStateException("Product is not available at location");
        }

        if (availability.getStockQuantity() < quantity) {
            throw new InsufficientStockException("Not enough stock at location");
        }

        // Update stock
        int newStock = availability.getStockQuantity() - quantity;
        availability.setStockQuantity(newStock);
        availability.setUpdatedAt(LocalDateTime.now());

        // If stock is 0, mark as unavailable
        if (newStock == 0) {
            availability.setAvailable(false);
        }

        locationAvailabilityRepository.save(availability);

        // Update product total stock
        updateProductTotalStock(productId);

        log.info("Reduced stock for product {} at location {} by {}, new stock: {}",
                productId, locationId, quantity, newStock);
    }

    /**
     * Increase stock at a specific location
     */
    @Transactional
    public void increaseStockAtLocation(UUID productId, UUID locationId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductLocationAvailabilityEntity availability = locationAvailabilityRepository
                .findByProductIdAndLocationId(productId, locationId)
                .orElseGet(() -> {
                    // Create new availability record
                    ProductLocationAvailabilityEntity newAvailability = new ProductLocationAvailabilityEntity();
                    newAvailability.setProductId(productId);
                    newAvailability.setLocationId(locationId);
                    newAvailability.setStockQuantity(0);
                    newAvailability.setAvailable(true);
                    newAvailability.setCreatedAt(LocalDateTime.now());
                    newAvailability.setUpdatedAt(LocalDateTime.now());
                    return newAvailability;
                });

        // Update stock
        int newStock = availability.getStockQuantity() + quantity;
        availability.setStockQuantity(newStock);
        availability.setUpdatedAt(LocalDateTime.now());

        // If previously unavailable, mark as available
        if (!availability.isAvailable() && newStock > 0) {
            availability.setAvailable(true);
        }

        locationAvailabilityRepository.save(availability);

        // Update product total stock
        updateProductTotalStock(productId);

        log.info("Increased stock for product {} at location {} by {}, new stock: {}",
                productId, locationId, quantity, newStock);
    }

    /**
     * Reduce overall product stock
     */
    @Transactional
    public void reduceStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getAvailableStock() == null) {
            product.setAvailableStock(0);
        } else if (product.getAvailableStock() < quantity) {
            throw new InsufficientStockException("Not enough stock available");
        }

        // Update stock
        int newStock = product.getAvailableStock() - quantity;
        product.setAvailableStock(newStock);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        log.info("Reduced stock for product {} by {}, new stock: {}",
                productId, quantity, newStock);
    }

    /**
     * Increase overall product stock
     */
    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getAvailableStock() == null) {
            product.setAvailableStock(quantity);
        } else {
            product.setAvailableStock(product.getAvailableStock() + quantity);
        }

        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("Increased stock for product {} by {}, new stock: {}",
                productId, quantity, product.getAvailableStock());
    }

    /**
     * Update product total stock based on location availability
     */
    @Transactional
    public void updateProductTotalStock(UUID productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Calculate total stock from all locations
        int totalStock = locationAvailabilityRepository.findAllByProductId(productId)
                .stream()
                .filter(ProductLocationAvailabilityEntity::isAvailable)
                .mapToInt(ProductLocationAvailabilityEntity::getStockQuantity)
                .sum();

        product.setAvailableStock(totalStock);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        log.info("Updated total stock for product {}: {}", productId, totalStock);
    }
}