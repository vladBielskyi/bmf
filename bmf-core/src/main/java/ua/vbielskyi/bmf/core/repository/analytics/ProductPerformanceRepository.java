package ua.vbielskyi.bmf.core.repository.analytics;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.analytics.ProductPerformanceEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductPerformanceRepository extends MultiTenantJpaRepository<ProductPerformanceEntity, UUID> {

    List<ProductPerformanceEntity> findAllByTenantIdAndPeriodStartAndPeriodEnd(
            UUID tenantId, LocalDate periodStart, LocalDate periodEnd);

    @Query("SELECT pp FROM ProductPerformanceEntity pp WHERE pp.tenantId = :tenantId " +
            "AND pp.periodStart = :periodStart AND pp.periodEnd = :periodEnd " +
            "ORDER BY pp.unitsSold DESC")
    List<ProductPerformanceEntity> findTopSellingProductsByTenantIdAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}