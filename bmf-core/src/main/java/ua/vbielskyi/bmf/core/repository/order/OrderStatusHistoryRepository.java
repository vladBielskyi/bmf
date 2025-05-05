package ua.vbielskyi.bmf.core.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.order.OrderStatusHistoryEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, UUID> {

    List<OrderStatusHistoryEntity> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);
}