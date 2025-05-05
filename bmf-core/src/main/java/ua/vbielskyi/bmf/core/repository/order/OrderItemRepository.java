package ua.vbielskyi.bmf.core.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findAllByOrderId(UUID orderId);

    void deleteAllByOrderId(UUID orderId);
}