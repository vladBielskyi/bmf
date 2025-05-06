package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.core.entity.order.DeliveryEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantLocationEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.order.DeliveryRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantLocationRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderWorkflowService orderWorkflowService;
    private final TenantLocationRepository locationRepository;
    private final LocationService locationService;
    private final NotificationService notificationService;

    /**
     * Create a delivery for an order
     */
    @Transactional
    public DeliveryEntity createDelivery(UUID orderId, String deliveryAddress, String recipientName,
                                         String recipientPhone, String deliveryNotes,
                                         LocalDateTime scheduledDeliveryTime) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Create delivery
        DeliveryEntity delivery = new DeliveryEntity();
        delivery.setOrderId(orderId);
        delivery.setDeliveryAddress(deliveryAddress);
        delivery.setRecipientName(recipientName);
        delivery.setRecipientPhone(recipientPhone);
        delivery.setDeliveryNotes(deliveryNotes);
        delivery.setScheduledDeliveryTime(scheduledDeliveryTime);
        delivery.setDeliveryStatus("SCHEDULED");
        delivery.setCreatedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());

        // Try to geocode the address for coordinates
        try {
            Map<String, Double> coordinates = locationService.geocodeAddress(deliveryAddress);
            delivery.setLatitude(coordinates.get("latitude"));
            delivery.setLongitude(coordinates.get("longitude"));
        } catch (Exception e) {
            log.warn("Could not geocode delivery address: {}", deliveryAddress, e);
            // Continue without coordinates
        }

        DeliveryEntity savedDelivery = deliveryRepository.save(delivery);

        // Update order
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryTime(scheduledDeliveryTime);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Created delivery for order: {}", orderId);

        return savedDelivery;
    }

    /**
     * Assign courier to delivery
     */
    @Transactional
    public DeliveryEntity assignCourier(UUID deliveryId, UUID courierId, String courierName) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        delivery.setCourierId(courierId);
        delivery.setCourierName(courierName);
        delivery.setDeliveryStatus("ASSIGNED");
        delivery.setUpdatedAt(LocalDateTime.now());

        DeliveryEntity updatedDelivery = deliveryRepository.save(delivery);

        // Update order status if needed
        OrderEntity order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", delivery.getOrderId()));

        if (order.getStatus() == OrderStatus.READY_FOR_DELIVERY) {
            orderWorkflowService.updateOrderStatus(
                    order.getId(), OrderStatus.OUT_FOR_DELIVERY, "delivery-system");
        }

        // Send notification to customer
        sendDeliveryAssignedNotification(order, courierName);

        log.info("Assigned courier {} to delivery: {}", courierId, deliveryId);

        return updatedDelivery;
    }

    /**
     * Start delivery
     */
    @Transactional
    public DeliveryEntity startDelivery(UUID deliveryId) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        delivery.setDeliveryStatus("IN_PROGRESS");
        delivery.setUpdatedAt(LocalDateTime.now());

        DeliveryEntity updatedDelivery = deliveryRepository.save(delivery);

        // Update order status
        OrderEntity order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", delivery.getOrderId()));

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            orderWorkflowService.updateOrderStatus(
                    order.getId(), OrderStatus.OUT_FOR_DELIVERY, "delivery-system");
        }

        // Send notification to customer
        sendDeliveryStartedNotification(order);

        log.info("Started delivery: {}", deliveryId);

        return updatedDelivery;
    }

    /**
     * Complete delivery
     */
    @Transactional
    public DeliveryEntity completeDelivery(UUID deliveryId) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        delivery.setDeliveryStatus("COMPLETED");
        delivery.setActualDeliveryTime(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());

        DeliveryEntity updatedDelivery = deliveryRepository.save(delivery);

        // Update order status
        OrderEntity order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", delivery.getOrderId()));

        orderWorkflowService.updateOrderStatus(
                order.getId(), OrderStatus.DELIVERED, "delivery-system");

        // Send notification to customer
        sendDeliveryCompletedNotification(order);

        log.info("Completed delivery: {}", deliveryId);

        return updatedDelivery;
    }

    /**
     * Get pending deliveries for a courier
     */
    public List<DeliveryEntity> getCourierPendingDeliveries(UUID courierId) {
        return deliveryRepository.findByCourierId(courierId).stream()
                .filter(d -> !d.getDeliveryStatus().equals("COMPLETED"))
                .collect(Collectors.toList());
    }

    /**
     * Get optimal delivery schedule for a day
     */
    public List<DeliveryEntity> getOptimalDeliverySchedule(UUID tenantId, LocalDateTime date) {
        LocalDateTime startOfDay = date.with(LocalTime.MIN);
        LocalDateTime endOfDay = date.with(LocalTime.MAX);

        // Get all deliveries scheduled for the day
        List<DeliveryEntity> deliveries = deliveryRepository
                .findByScheduledDeliveryTimeBetween(startOfDay, endOfDay);

        // Filter by tenant
        List<DeliveryEntity> tenantDeliveries = deliveries.stream()
                .filter(d -> {
                    OrderEntity order = orderRepository.findById(d.getOrderId()).orElse(null);
                    return order != null && order.getTenantId().equals(tenantId);
                })
                .collect(Collectors.toList());

        // Get tenant location for starting point
        List<TenantLocationEntity> locations = locationRepository.findAllByTenantIdAndActiveTrue(tenantId);
        if (locations.isEmpty()) {
            return tenantDeliveries; // No optimization if no locations
        }

        TenantLocationEntity startLocation = locations.get(0);

        // Perform simple optimization based on distance
        if (startLocation.getLatitude() != null && startLocation.getLongitude() != null) {
            return optimizeDeliveryRouteByDistance(tenantDeliveries,
                    startLocation.getLatitude(), startLocation.getLongitude());
        }

        return tenantDeliveries;
    }

    /**
     * Simple greedy algorithm to optimize delivery route by distance
     */
    private List<DeliveryEntity> optimizeDeliveryRouteByDistance(
            List<DeliveryEntity> deliveries, Double startLat, Double startLng) {

        if (deliveries.isEmpty()) {
            return deliveries;
        }

        List<DeliveryEntity> optimizedRoute = new ArrayList<>();
        List<DeliveryEntity> remaining = new ArrayList<>(deliveries);

        double currentLat = startLat;
        double currentLng = startLng;

        while (!remaining.isEmpty()) {
            // Find nearest delivery
            DeliveryEntity nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (DeliveryEntity delivery : remaining) {
                if (delivery.getLatitude() != null && delivery.getLongitude() != null) {
                    double distance = calculateDistance(
                            currentLat, currentLng,
                            delivery.getLatitude(), delivery.getLongitude());

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearest = delivery;
                    }
                }
            }

            // If we couldn't find a delivery with coordinates, just take the first one
            if (nearest == null) {
                nearest = remaining.get(0);
            } else {
                // Update current position
                currentLat = nearest.getLatitude();
                currentLng = nearest.getLongitude();
            }

            optimizedRoute.add(nearest);
            remaining.remove(nearest);
        }

        return optimizedRoute;
    }

    /**
     * Calculate distance between two coordinates (Haversine formula)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Send delivery assigned notification
     */
    private void sendDeliveryAssignedNotification(OrderEntity order, String courierName) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("courierName", courierName);
        templateData.put("deliveryAddress", order.getDeliveryAddress());
        templateData.put("deliveryTime", order.getDeliveryTime());

        notificationService.sendDeliveryUpdateNotification(order, templateData);
    }

    /**
     * Send delivery started notification
     */
    private void sendDeliveryStartedNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("status", "on its way");
        templateData.put("deliveryAddress", order.getDeliveryAddress());

        notificationService.sendDeliveryUpdateNotification(order, templateData);
    }

    /**
     * Send delivery completed notification
     */
    private void sendDeliveryCompletedNotification(OrderEntity order) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", order.getOrderNumber());
        templateData.put("status", "delivered");

        notificationService.sendDeliveryCompletedNotification(order);
    }
}