package ua.vbielskyi.bmf.core.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends MultiTenantJpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    List<OrderEntity> findAllByTenantIdAndStatusAndDeletedFalse(UUID tenantId, OrderStatus status);

    List<OrderEntity> findAllByTenantIdAndCustomerIdAndDeletedFalse(UUID tenantId, UUID customerId);

    List<OrderEntity> findAllByTenantIdAndCustomerTelegramIdAndDeletedFalse(UUID tenantId, Long customerTelegramId);

    Page<OrderEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.tenantId = :tenantId AND o.createdAt BETWEEN :startDate AND :endDate AND o.deleted = false")
    List<OrderEntity> findAllByTenantIdAndDateRangeAndDeletedFalse(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM OrderEntity o WHERE o.tenantId = :tenantId AND o.locationId = :locationId AND o.deleted = false")
    List<OrderEntity> findAllByTenantIdAndLocationIdAndDeletedFalse(
            @Param("tenantId") UUID tenantId,
            @Param("locationId") UUID locationId);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.tenantId = :tenantId AND o.deleted = false")
    Long countByTenantIdAndDeletedFalse(@Param("tenantId") UUID tenantId);
}