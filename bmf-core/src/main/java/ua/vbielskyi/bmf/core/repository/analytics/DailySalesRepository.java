package ua.vbielskyi.bmf.core.repository.analytics;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.analytics.DailySalesEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailySalesRepository extends MultiTenantJpaRepository<DailySalesEntity, UUID> {

    List<DailySalesEntity> findAllByTenantIdAndDateBetween(
            UUID tenantId, LocalDate startDate, LocalDate endDate);

    List<DailySalesEntity> findAllByTenantIdAndLocationIdAndDateBetween(
            UUID tenantId, UUID locationId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(ds.totalSales) FROM DailySalesEntity ds WHERE ds.tenantId = :tenantId AND ds.date BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalSalesByTenantIdAndDateBetween(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}