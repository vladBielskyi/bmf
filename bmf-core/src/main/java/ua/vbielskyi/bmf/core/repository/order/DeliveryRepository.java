package ua.vbielskyi.bmf.core.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.order.DeliveryEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {

    Optional<DeliveryEntity> findByOrderId(UUID orderId);

    List<DeliveryEntity> findByCourierId(UUID courierId);

    List<DeliveryEntity> findByDeliveryStatus(String status);

    List<DeliveryEntity> findByScheduledDeliveryTimeBetween(
            LocalDateTime startTime, LocalDateTime endTime);
}