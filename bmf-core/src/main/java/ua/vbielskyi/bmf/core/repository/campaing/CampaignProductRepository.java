package ua.vbielskyi.bmf.core.repository.campaing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.campaing.CampaignProductEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignProductRepository extends JpaRepository<CampaignProductEntity, UUID> {

    List<CampaignProductEntity> findAllByCampaignId(UUID campaignId);

    List<CampaignProductEntity> findAllByProductId(UUID productId);

    boolean existsByCampaignIdAndProductId(UUID campaignId, UUID productId);

    void deleteByCampaignIdAndProductId(UUID campaignId, UUID productId);
}