package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductImageEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {

    List<ProductImageEntity> findAllByProductIdOrderBySortOrder(UUID productId);

    Optional<ProductImageEntity> findByProductIdAndMainTrue(UUID productId);

    void deleteAllByProductId(UUID productId);
}