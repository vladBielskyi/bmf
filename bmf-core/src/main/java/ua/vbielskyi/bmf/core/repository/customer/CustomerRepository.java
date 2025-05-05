package ua.vbielskyi.bmf.core.repository.customer;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.tenant.repository.MultiTenantJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends MultiTenantJpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByTenantIdAndTelegramId(UUID tenantId, Long telegramId);

    List<CustomerEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId AND " +
            "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<CustomerEntity> searchCustomers(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);
}