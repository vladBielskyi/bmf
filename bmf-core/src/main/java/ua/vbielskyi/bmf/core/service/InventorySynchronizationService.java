package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductLocationAvailabilityEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantLocationEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.product.ProductLocationAvailabilityRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantLocationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySynchronizationService {

    private final ProductRepository productRepository;
    private final ProductLocationAvailabilityRepository availabilityRepository;
    private final TenantLocationRepository locationRepository;

    /**
     * Synchronize product availability across all tenant locations
     */
    @Transactional
    public void synchronizeProductAvailability(UUID tenantId, UUID productId) {
        ProductEntity product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        List<TenantLocationEntity> locations = locationRepository.findAllByTenantIdAndActiveTrue(tenantId);

        // Update total available stock based on all locations
        int totalStock = calculateTotalAvailableStock(productId);
        product.setAvailableStock(totalStock);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("Synchronized inventory for product: {}, tenant: {}, total stock: {}",
                productId, tenantId, totalStock);
    }

    /**
     * Transfer inventory between locations
     */
    @Transactional
    public void transferInventory(UUID productId, UUID sourceLocationId, UUID targetLocationId, int quantity) {
        // Validate inputs
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        // Get source and target availability records
        ProductLocationAvailabilityEntity sourceAvailability = availabilityRepository
                .findByProductIdAndLocationId(productId, sourceLocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not available at source location"));

        if (sourceAvailability.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock at source location");
        }

        // Update source inventory
        sourceAvailability.setStockQuantity(sourceAvailability.getStockQuantity() - quantity);
        sourceAvailability.setUpdatedAt(LocalDateTime.now());
        availabilityRepository.save(sourceAvailability);

        // Update or create target inventory
        ProductLocationAvailabilityEntity targetAvailability = availabilityRepository
                .findByProductIdAndLocationId(productId, targetLocationId)
                .orElseGet(() -> {
                    ProductLocationAvailabilityEntity newAvailability = new ProductLocationAvailabilityEntity();
                    newAvailability.setProductId(productId);
                    newAvailability.setLocationId(targetLocationId);
                    newAvailability.setAvailable(true);
                    newAvailability.setCreatedAt(LocalDateTime.now());
                    return newAvailability;
                });

        targetAvailability.setStockQuantity(targetAvailability.getStockQuantity() + quantity);
        targetAvailability.setUpdatedAt(LocalDateTime.now());
        availabilityRepository.save(targetAvailability);

        log.info("Transferred {} units of product {} from location {} to location {}",
                quantity, productId, sourceLocationId, targetLocationId);
    }

    /**
     * Calculate total available stock across all locations
     */
    private int calculateTotalAvailableStock(UUID productId) {
        List<ProductLocationAvailabilityEntity> availabilities = availabilityRepository.findAllByProductId(productId);

        return availabilities.stream()
                .filter(ProductLocationAvailabilityEntity::isAvailable)
                .mapToInt(ProductLocationAvailabilityEntity::getStockQuantity)
                .sum();
    }
}