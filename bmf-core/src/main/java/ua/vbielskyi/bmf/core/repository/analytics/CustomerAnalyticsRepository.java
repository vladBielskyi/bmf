package ua.vbielskyi.bmf.core.repository.analytics;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.analytics.CustomerAnalyticsEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerAnalyticsRepository extends MultiTenantJpaRepository<CustomerAnalyticsEntity, UUID> {

    List<CustomerAnalyticsEntity> findAllByTenantIdOrderByTotalSpentDesc(UUID tenantId);

    @Query("SELECT ca FROM CustomerAnalyticsEntity ca WHERE ca.tenantId = :tenantId " +
            "ORDER BY ca.totalSpent DESC LIMIT :limit")
    List<CustomerAnalyticsEntity> findTopCustomersByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("limit") int limit);

    @Query("SELECT AVG(ca.totalSpent) FROM CustomerAnalyticsEntity ca WHERE ca.tenantId = :tenantId")
    BigDecimal calculateAverageCustomerLifetimeValue(@Param("tenantId") UUID tenantId);
}