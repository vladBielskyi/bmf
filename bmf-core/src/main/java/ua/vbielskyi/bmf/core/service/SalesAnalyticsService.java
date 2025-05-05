package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.core.entity.analytics.DailySalesEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;
import ua.vbielskyi.bmf.core.repository.analytics.DailySalesRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderItemRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final DailySalesRepository dailySalesRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Generate or update daily sales report
     */
    public DailySalesEntity generateDailySalesReport(UUID tenantId, LocalDate date, UUID locationId) {
        log.info("Generating daily sales report for tenant: {}, date: {}, location: {}",
                tenantId, date, locationId);

        // Set time boundaries for the day
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        // Find all completed or delivered orders for the day
        List<OrderEntity> orders;
        if (locationId != null) {
            orders = orderRepository.findAllByTenantIdAndLocationIdAndDeletedFalse(tenantId, locationId)
                    .stream()
                    .filter(order -> order.getCreatedAt().isAfter(startDateTime) &&
                            order.getCreatedAt().isBefore(endDateTime))
                    .filter(order -> order.getStatus() == OrderStatus.COMPLETED ||
                            order.getStatus() == OrderStatus.DELIVERED)
                    .collect(Collectors.toList());
        } else {
            orders = orderRepository.findAllByTenantIdAndDateRangeAndDeletedFalse(
                            tenantId, startDateTime, endDateTime)
                    .stream()
                    .filter(order -> order.getStatus() == OrderStatus.COMPLETED ||
                            order.getStatus() == OrderStatus.DELIVERED)
                    .collect(Collectors.toList());
        }

        if (orders.isEmpty()) {
            log.info("No orders found for report criteria");

            // Create empty report
            DailySalesEntity emptySalesReport = DailySalesEntity.builder()
                    .tenantId(tenantId)
                    .locationId(locationId)
                    .date(date)
                    .orderCount(0)
                    .productCount(0)
                    .totalSales(BigDecimal.ZERO)
                    .averageOrderValue(BigDecimal.ZERO)
                    .discountsTotal(BigDecimal.ZERO)
                    .build();

            return dailySalesRepository.save(emptySalesReport);
        }

        // Calculate report metrics
        int orderCount = orders.size();

        BigDecimal totalSales = orders.stream()
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountsTotal = orders.stream()
                .map(OrderEntity::getDiscountAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (orderCount > 0) {
            averageOrderValue = totalSales.divide(
                    BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        }

        // Count total products sold
        int productCount = 0;
        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());
            productCount += items.stream()
                    .mapToInt(OrderItemEntity::getQuantity)
                    .sum();
        }

        // Check if report already exists for this day/location
        Optional<DailySalesEntity> existingReport;
        if (locationId != null) {
            existingReport = dailySalesRepository.findAllByTenantIdAndLocationIdAndDateBetween(
                            tenantId, locationId, date, date)
                    .stream()
                    .findFirst();
        } else {
            existingReport = dailySalesRepository.findAllByTenantIdAndDateBetween(
                            tenantId, date, date)
                    .stream()
                    .filter(report -> report.getLocationId() == null)
                    .findFirst();
        }

        // Update existing or create new report
        DailySalesEntity salesReport;
        if (existingReport.isPresent()) {
            salesReport = existingReport.get();
            salesReport.setOrderCount(orderCount);
            salesReport.setProductCount(productCount);
            salesReport.setTotalSales(totalSales);
            salesReport.setAverageOrderValue(averageOrderValue);
            salesReport.setDiscountsTotal(discountsTotal);
        } else {
            salesReport = DailySalesEntity.builder()
                    .tenantId(tenantId)
                    .locationId(locationId)
                    .date(date)
                    .orderCount(orderCount)
                    .productCount(productCount)
                    .totalSales(totalSales)
                    .averageOrderValue(averageOrderValue)
                    .discountsTotal(discountsTotal)
                    .build();
        }

        return dailySalesRepository.save(salesReport);
    }

    /**
     * Get sales for date range
     */
    public Map<String, Object> getSalesReportForDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        List<DailySalesEntity> salesData = dailySalesRepository.findAllByTenantIdAndDateBetween(
                tenantId, startDate, endDate);

        if (salesData.isEmpty()) {
            return createEmptySalesReport(startDate, endDate);
        }

        // Total metrics
        int totalOrders = salesData.stream()
                .mapToInt(DailySalesEntity::getOrderCount)
                .sum();

        int totalProducts = salesData.stream()
                .mapToInt(DailySalesEntity::getProductCount)
                .sum();

        BigDecimal totalRevenue = salesData.stream()
                .map(DailySalesEntity::getTotalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscounts = salesData.stream()
                .map(DailySalesEntity::getDiscountsTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate daily average
        int dayCount = (int) salesData.stream()
                .map(DailySalesEntity::getDate)
                .distinct()
                .count();

        BigDecimal dailyAverage = BigDecimal.ZERO;
        if (dayCount > 0) {
            dailyAverage = totalRevenue.divide(
                    BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP);
        }

        // Calculate average order value
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            averageOrderValue = totalRevenue.divide(
                    BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        // Prepare daily data for chart
        Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();
        for (DailySalesEntity sale : salesData) {
            dailyRevenue.merge(sale.getDate(), sale.getTotalSales(), BigDecimal::add);
        }

        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyRevenue.entrySet()) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", entry.getKey().toString());
            day.put("revenue", entry.getValue());
            dailyData.add(day);
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalOrders", totalOrders);
        result.put("totalProducts", totalProducts);
        result.put("totalRevenue", totalRevenue);
        result.put("totalDiscounts", totalDiscounts);
        result.put("dailyAverage", dailyAverage);
        result.put("averageOrderValue", averageOrderValue);
        result.put("dailyData", dailyData);

        return result;
    }

    /**
     * Create empty sales report for date range
     */
    private Map<String, Object> createEmptySalesReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalOrders", 0);
        result.put("totalProducts", 0);
        result.put("totalRevenue", BigDecimal.ZERO);
        result.put("totalDiscounts", BigDecimal.ZERO);
        result.put("dailyAverage", BigDecimal.ZERO);
        result.put("averageOrderValue", BigDecimal.ZERO);
        result.put("dailyData", Collections.emptyList());

        return result;
    }
}