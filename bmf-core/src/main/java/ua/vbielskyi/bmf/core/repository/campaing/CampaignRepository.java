package ua.vbielskyi.bmf.core.repository.campaing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.common.model.campaing.CampaignType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID> {

    List<CampaignEntity> findByTenantId(UUID tenantId);

    List<CampaignEntity> findByTenantIdAndActiveTrue(UUID tenantId);

    List<CampaignEntity> findByTenantIdAndType(UUID tenantId, CampaignType type);

    List<CampaignEntity> findByActiveAndStartDateBeforeAndEndDateAfter(
            boolean active, LocalDateTime now, LocalDateTime now1);

    List<CampaignEntity> findByActiveAndEndDateBefore(boolean active, LocalDateTime now);
}