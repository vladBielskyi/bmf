package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductTagMappingEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductTagMappingRepository extends JpaRepository<ProductTagMappingEntity, UUID> {

    List<ProductTagMappingEntity> findAllByProductId(UUID productId);

    List<ProductTagMappingEntity> findAllByTagId(UUID tagId);

    void deleteAllByProductId(UUID productId);

    void deleteByProductIdAndTagId(UUID productId, UUID tagId);
}