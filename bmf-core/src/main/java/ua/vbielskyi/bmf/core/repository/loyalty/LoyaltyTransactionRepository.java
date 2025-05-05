package ua.vbielskyi.bmf.core.repository.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyTransactionEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransactionEntity, UUID> {

    List<LoyaltyTransactionEntity> findAllByCustomerId(UUID customerId);

    List<LoyaltyTransactionEntity> findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    List<LoyaltyTransactionEntity> findAllByTenantIdAndOrderId(UUID tenantId, UUID orderId);
}