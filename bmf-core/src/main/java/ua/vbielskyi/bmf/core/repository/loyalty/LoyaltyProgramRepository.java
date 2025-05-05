package ua.vbielskyi.bmf.core.repository.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyProgramEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgramEntity, UUID> {

    Optional<LoyaltyProgramEntity> findByTenantId(UUID tenantId);
}