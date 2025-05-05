package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductCategoryEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends MultiTenantJpaRepository<ProductCategoryEntity, UUID> {

    List<ProductCategoryEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    List<ProductCategoryEntity> findAllByTenantIdOrderBySortOrder(UUID tenantId);
}