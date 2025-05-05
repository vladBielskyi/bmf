package ua.vbielskyi.bmf.core.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderStatusHistoryEntity;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderStatusHistoryRepository;
import ua.vbielskyi.bmf.core.service.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final NotificationService notificationService;

    /**
     * Update order status with validation and notifications
     */
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, OrderStatus newStatus, String updatedBy) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus oldStatus = order.getStatus();

        // Validate status transition
        if (!order.isValidStatusTransition(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid order status transition from %s to %s", oldStatus, newStatus));
        }

        // Update order status
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(updatedBy);
        orderRepository.save(order);

        // Record status change in history
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(orderId);
        history.setPreviousStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setUpdatedBy(updatedBy);
        history.setCreatedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);

        // Send notifications
        sendStatusChangeNotifications(order, oldStatus, newStatus);

        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        return order;
    }

    /**
     * Send appropriate notifications based on status change
     */
    private void sendStatusChangeNotifications(OrderEntity order, OrderStatus oldStatus, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED:
                notificationService.sendOrderConfirmation(order);
                break;

            case PROCESSING:
                notificationService.sendOrderProcessingUpdate(order);
                break;

            case READY_FOR_DELIVERY:
                notificationService.sendOrderReadyNotification(order);
                break;

            case OUT_FOR_DELIVERY:
                notificationService.sendOutForDeliveryNotification(order);
                break;

            case DELIVERED:
                notificationService.sendDeliveryCompletedNotification(order);
                break;

            case COMPLETED:
                notificationService.sendOrderCompletedNotification(order);
                // Also send feedback request
                notificationService.sendFeedbackRequest(order);
                break;

            case CANCELLED:
                notificationService.sendCancellationNotification(order);
                break;

            case REFUNDED:
                notificationService.sendRefundNotification(order);
                break;

            default:
                // No notification for other statuses
                break;
        }
    }
}