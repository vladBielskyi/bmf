package ua.vbielskyi.bmf.core.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.common.model.order.PaymentMethod;
import ua.vbielskyi.bmf.common.model.order.PaymentStatus;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.exception.InsufficientStockException;
import ua.vbielskyi.bmf.core.exception.OrderNotFoundException;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderItemRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;
import ua.vbielskyi.bmf.core.service.InventoryService;
import ua.vbielskyi.bmf.core.service.OrderNumberGenerator;
import ua.vbielskyi.bmf.core.service.cart.ShoppingCartService;
import ua.vbielskyi.bmf.core.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final ShoppingCartService cartService;
    private final NotificationService notificationService;

    /**
     * Create new order from shopping cart
     */
    @Transactional
    public OrderEntity createOrderFromCart(UUID tenantId, Long telegramId, UUID locationId,
                                           PaymentMethod paymentMethod, String deliveryAddress) {
        // Get customer
        CustomerEntity customer = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Get shopping cart
        ShoppingCartService.Cart cart = cartService.getCart(tenantId, telegramId);

        if (cart.getItemsList().isEmpty()) {
            throw new IllegalStateException("Cannot create order with empty cart");
        }

        // Validate product availability and gather product information
        List<OrderItemEntity> orderItems = new ArrayList<>();

        for (ShoppingCartService.CartItem cartItem : cart.getItemsList()) {
            ProductEntity product = productRepository.findByIdAndTenantId(cartItem.getProductId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProductId()));

            // Check if product is active
            if (!product.isActive()) {
                throw new IllegalStateException("Product is not active: " + product.getName());
            }

            // Check stock availability at specified location if given
            if (locationId != null) {
                if (!inventoryService.isProductAvailableAtLocation(product.getId(), locationId, cartItem.getQuantity())) {
                    throw new InsufficientStockException(
                            "Insufficient stock for product: " + product.getName() + " at specified location");
                }
            } else {
                // Check overall stock
                if (product.getAvailableStock() != null && product.getAvailableStock() < cartItem.getQuantity()) {
                    throw new InsufficientStockException(
                            "Insufficient stock for product: " + product.getName());
                }
            }

            // Create order item
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImageUrl());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItem.setTotalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItems.add(orderItem);
        }

        // Calculate order totals
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        OrderEntity order = new OrderEntity();
        order.setTenantId(tenantId);
        order.setLocationId(locationId);
        order.setCustomerId(customer.getId());
        order.setCustomerTelegramId(telegramId);
        order.setOrderNumber(orderNumberGenerator.generateOrderNumber(tenantId));
        order.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        order.setCustomerPhone(customer.getPhone());
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(totalAmount);
        order.setDeliveryAddress(deliveryAddress);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Save order
        OrderEntity savedOrder = orderRepository.save(order);

        // Save order items
        for (OrderItemEntity item : orderItems) {
            item.setOrderId(savedOrder.getId());
            orderItemRepository.save(item);

            // Update inventory
            if (locationId != null) {
                inventoryService.reduceStockAtLocation(item.getProductId(), locationId, item.getQuantity());
            } else {
                inventoryService.reduceStock(item.getProductId(), item.getQuantity());
            }
        }

        // Clear cart
        cartService.clearCart(tenantId, telegramId);

        // Send order confirmation notification
        notificationService.sendOrderConfirmation(savedOrder);

        log.info("Created new order: {} for customer: {}, items: {}",
                savedOrder.getId(), customer.getId(), orderItems.size());

        return savedOrder;
    }

    /**
     * Get order by ID
     */
    public OrderEntity getOrder(UUID tenantId, UUID orderId) {
        return orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Get customer orders
     */
    public List<OrderEntity> getCustomerOrders(UUID tenantId, Long telegramId) {
        return orderRepository.findAllByTenantIdAndCustomerTelegramIdAndDeletedFalse(tenantId, telegramId);
    }

    /**
     * Get order items
     */
    public List<OrderItemEntity> getOrderItems(UUID orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    /**
     * Update order status
     */
    @Transactional
    public OrderEntity updateOrderStatus(UUID tenantId, UUID orderId, OrderStatus newStatus, String updatedBy) {
        OrderEntity order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();

        // Validate status transition
        if (!order.isValidStatusTransition(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid order status transition from %s to %s", previousStatus, newStatus));
        }

        // Update order
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(updatedBy);

        OrderEntity updatedOrder = orderRepository.save(order);

        // Record status change in history
        recordStatusChange(orderId, previousStatus, newStatus, updatedBy);

        // Send notification based on new status
        sendStatusUpdateNotification(updatedOrder, previousStatus);

        log.info("Updated order {} status from {} to {}", orderId, previousStatus, newStatus);

        return updatedOrder;
    }

    /**
     * Record status change in history
     */
    private void recordStatusChange(UUID orderId, OrderStatus previousStatus,
                                    OrderStatus newStatus, String updatedBy) {
        // Implementation would create a record in OrderStatusHistoryEntity
    }

    /**
     * Send notification based on status change
     */
    private void sendStatusUpdateNotification(OrderEntity order, OrderStatus previousStatus) {
        switch (order.getStatus()) {
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
                break;

            case CANCELLED:
                notificationService.sendCancellationNotification(order);
                break;

            case REFUNDED:
                notificationService.sendRefundNotification(order);
                break;

            default:
                // No notification for other status changes
                break;
        }
    }

    /**
     * Cancel order
     */
    @Transactional
    public OrderEntity cancelOrder(UUID tenantId, UUID orderId, String reason, String cancelledBy) {
        OrderEntity order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Can only cancel orders in NEW or CONFIRMED status
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in " + order.getStatus() + " status");
        }

        OrderStatus previousStatus = order.getStatus();

        // Update order
        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes(order.getNotes() != null ?
                order.getNotes() + "\nCancellation reason: " + reason :
                "Cancellation reason: " + reason);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(cancelledBy);

        OrderEntity cancelledOrder = orderRepository.save(order);

        // Record status change in history
        recordStatusChange(orderId, previousStatus, OrderStatus.CANCELLED, cancelledBy);

        // Restore inventory
        List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(orderId);
        for (OrderItemEntity item : items) {
            if (order.getLocationId() != null) {
                inventoryService.increaseStockAtLocation(
                        item.getProductId(), order.getLocationId(), item.getQuantity());
            } else {
                inventoryService.increaseStock(item.getProductId(), item.getQuantity());
            }
        }

        // Send cancellation notification
        notificationService.sendCancellationNotification(cancelledOrder);

        log.info("Cancelled order {}, reason: {}", orderId, reason);

        return cancelledOrder;
    }
}