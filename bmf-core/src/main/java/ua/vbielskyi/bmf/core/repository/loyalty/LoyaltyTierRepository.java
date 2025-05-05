package ua.vbielskyi.bmf.core.repository.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.loyalty.LoyaltyTierEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTierEntity, UUID> {

    List<LoyaltyTierEntity> findAllByProgramIdOrderByRequiredPointsDesc(UUID programId);

    List<LoyaltyTierEntity> findAllByProgramIdOrderByRequiredPointsAsc(UUID programId);
}