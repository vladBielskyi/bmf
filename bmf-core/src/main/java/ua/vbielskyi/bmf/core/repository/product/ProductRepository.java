package ua.vbielskyi.bmf.core.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends MultiTenantJpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Page<ProductEntity> findAllByTenantIdAndActiveTrue(UUID tenantId, Pageable pageable);

    List<ProductEntity> findAllByTenantIdAndCategoryIdAndActiveTrue(UUID tenantId, UUID categoryId);

    List<ProductEntity> findAllByTenantIdAndFeaturedTrueAndActiveTrue(UUID tenantId);

    @Query("SELECT p FROM ProductEntity p JOIN ProductTagMappingEntity ptm ON p.id = ptm.productId " +
            "JOIN ProductTagEntity pt ON ptm.tagId = pt.id " +
            "WHERE p.tenantId = :tenantId AND pt.name = :tagName AND p.active = true")
    List<ProductEntity> findAllByTenantIdAndTagNameAndActiveTrue(
            @Param("tenantId") UUID tenantId,
            @Param("tagName") String tagName);

    @Query("SELECT p FROM ProductEntity p JOIN ProductLocationAvailabilityEntity pla ON p.id = pla.productId " +
            "WHERE p.tenantId = :tenantId AND pla.locationId = :locationId AND pla.available = true AND p.active = true")
    List<ProductEntity> findAllAvailableAtLocation(
            @Param("tenantId") UUID tenantId,
            @Param("locationId") UUID locationId);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
}