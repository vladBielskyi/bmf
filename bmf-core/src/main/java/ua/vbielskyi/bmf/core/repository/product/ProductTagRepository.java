package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductTagEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductTagRepository extends MultiTenantJpaRepository<ProductTagEntity, UUID> {

    List<ProductTagEntity> findAllByTenantId(UUID tenantId);

    Optional<ProductTagEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT pt FROM ProductTagEntity pt JOIN ProductTagMappingEntity ptm ON pt.id = ptm.tagId " +
            "WHERE ptm.productId = :productId")
    List<ProductTagEntity> findAllByProductId(@Param("productId") UUID productId);
}