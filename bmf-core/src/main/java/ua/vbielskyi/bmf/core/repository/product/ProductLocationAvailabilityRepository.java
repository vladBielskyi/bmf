package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductLocationAvailabilityEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductLocationAvailabilityRepository extends JpaRepository<ProductLocationAvailabilityEntity, UUID> {

    List<ProductLocationAvailabilityEntity> findAllByProductId(UUID productId);

    List<ProductLocationAvailabilityEntity> findAllByLocationId(UUID locationId);

    Optional<ProductLocationAvailabilityEntity> findByProductIdAndLocationId(UUID productId, UUID locationId);

    void deleteAllByProductId(UUID productId);

    void deleteAllByLocationId(UUID locationId);
}