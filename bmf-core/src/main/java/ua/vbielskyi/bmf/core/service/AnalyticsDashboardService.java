package ua.vbielskyi.bmf.core.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.core.entity.analytics.CustomerAnalyticsEntity;
import ua.vbielskyi.bmf.core.entity.analytics.DailySalesEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderItemEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductCategoryEntity;
import ua.vbielskyi.bmf.core.entity.product.ProductEntity;
import ua.vbielskyi.bmf.core.repository.analytics.CustomerAnalyticsRepository;
import ua.vbielskyi.bmf.core.repository.analytics.DailySalesRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderItemRepository;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductCategoryRepository;
import ua.vbielskyi.bmf.core.repository.product.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final DailySalesRepository dailySalesRepository;
    private final CustomerAnalyticsRepository customerAnalyticsRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    /**
     * Get dashboard summary data
     */
    public Map<String, Object> getDashboardSummary(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDateTime = endOfMonth.atTime(23, 59, 59);

        // Calculate today's sales
        DailySalesEntity todaySales = dailySalesRepository
                .findAllByTenantIdAndDateBetween(tenantId, today, today)
                .stream()
                .findFirst()
                .orElse(createEmptyDailySales(tenantId, today));

        // Calculate monthly sales
        BigDecimal monthlySales = dailySalesRepository
                .sumTotalSalesByTenantIdAndDateBetween(tenantId, startOfMonth, endOfMonth);
        if (monthlySales == null) {
            monthlySales = BigDecimal.ZERO;
        }

        // Calculate completed orders
        List<OrderEntity> completedOrders = orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.COMPLETED);

        // Calculate pending orders
        List<OrderEntity> pendingOrders = orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.NEW);
        pendingOrders.addAll(orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.CONFIRMED));

        // Calculate processing orders
        List<OrderEntity> processingOrders = orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.PROCESSING);
        processingOrders.addAll(orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.READY_FOR_DELIVERY));
        processingOrders.addAll(orderRepository
                .findAllByTenantIdAndStatusAndDeletedFalse(tenantId, OrderStatus.OUT_FOR_DELIVERY));

        // Get top 5 customers
        List<CustomerAnalyticsEntity> topCustomers = customerAnalyticsRepository
                .findTopCustomersByTenantId(tenantId, 5);

        // Get average order value
        BigDecimal avgOrderValue = customerAnalyticsRepository
                .calculateAverageCustomerLifetimeValue(tenantId);
        if (avgOrderValue == null) {
            avgOrderValue = BigDecimal.ZERO;
        }

        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("todaySales", todaySales.getTotalSales());
        result.put("todayOrders", todaySales.getOrderCount());
        result.put("monthlySales", monthlySales);
        result.put("completedOrdersCount", completedOrders.size());
        result.put("pendingOrdersCount", pendingOrders.size());
        result.put("processingOrdersCount", processingOrders.size());
        result.put("topCustomers", topCustomers);
        result.put("averageCustomerValue", avgOrderValue);

        // Add sales trends
        result.put("salesTrends", getSalesTrends(tenantId, startOfMonth, today));

        // Add top products
        result.put("topProducts", getTopProducts(tenantId, startOfMonthDateTime, endOfMonthDateTime, 5));

        // Add category distribution
        result.put("categoryDistribution", getCategoryDistribution(tenantId, startOfMonthDateTime, endOfMonthDateTime));

        return result;
    }

    /**
     * Get sales trends for a period
     */
    public List<Map<String, Object>> getSalesTrends(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        List<DailySalesEntity> salesData = dailySalesRepository
                .findAllByTenantIdAndDateBetween(tenantId, startDate, endDate);

        // Create a map to hold sales by date
        Map<LocalDate, DailySalesEntity> salesByDate = new HashMap<>();
        for (DailySalesEntity sales : salesData) {
            salesByDate.put(sales.getDate(), sales);
        }

        // Fill in missing dates
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DailySalesEntity sales = salesByDate.getOrDefault(
                    currentDate, createEmptyDailySales(tenantId, currentDate));

            Map<String, Object> dailyData = new HashMap<>();
            dailyData.put("date", currentDate.toString());
            dailyData.put("sales", sales.getTotalSales());
            dailyData.put("orders", sales.getOrderCount());
            result.add(dailyData);

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * Get top selling products
     */
    public List<Map<String, Object>> getTopProducts(UUID tenantId,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    int limit) {
        // Get orders within date range
        List<OrderEntity> orders = orderRepository
                .findAllByTenantIdAndDateRangeAndDeletedFalse(tenantId, startDate, endDate);

        // Calculate product sales
        Map<UUID, Integer> productQuantities = new HashMap<>();
        Map<UUID, BigDecimal> productRevenues = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED ||
                    order.getStatus() == OrderStatus.REFUNDED) {
                continue;
            }

            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                productQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                productRevenues.merge(item.getProductId(), item.getTotalPrice(), BigDecimal::add);
            }
        }

        // Sort products by quantity sold
        List<Map.Entry<UUID, Integer>> sortedProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Prepare result
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : sortedProducts) {
            UUID productId = entry.getKey();
            ProductEntity product = productRepository.findById(productId).orElse(null);

            if (product != null) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("productId", productId);
                productData.put("productName", product.getName());
                productData.put("quantitySold", entry.getValue());
                productData.put("revenue", productRevenues.get(productId));
                productData.put("imageUrl", product.getMainImageUrl());

                result.add(productData);
            }
        }

        return result;
    }

    /**
     * Get sales distribution by category
     */
    public List<Map<String, Object>> getCategoryDistribution(UUID tenantId,
                                                             LocalDateTime startDate,
                                                             LocalDateTime endDate) {
        // Get orders within date range
        List<OrderEntity> orders = orderRepository
                .findAllByTenantIdAndDateRangeAndDeletedFalse(tenantId, startDate, endDate);

        // Calculate category sales
        Map<UUID, BigDecimal> categoryRevenues = new HashMap<>();
        Map<UUID, Integer> categoryQuantities = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED ||
                    order.getStatus() == OrderStatus.REFUNDED) {
                continue;
            }

            List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                ProductEntity product = productRepository.findById(item.getProductId()).orElse(null);

                if (product != null && product.getCategoryId() != null) {
                    categoryRevenues.merge(product.getCategoryId(), item.getTotalPrice(), BigDecimal::add);
                    categoryQuantities.merge(product.getCategoryId(), item.getQuantity(), Integer::sum);
                }
            }
        }

        // Get category names
        List<ProductCategoryEntity> categories = categoryRepository.findAllByTenantId(tenantId);
        Map<UUID, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(ProductCategoryEntity::getId, ProductCategoryEntity::getName));

        // Calculate total revenue
        BigDecimal totalRevenue = categoryRevenues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Prepare result
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : categoryRevenues.entrySet()) {
            UUID categoryId = entry.getKey();
            BigDecimal revenue = entry.getValue();

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("categoryId", categoryId);
            categoryData.put("categoryName", categoryNames.getOrDefault(categoryId, "Unknown"));
            categoryData.put("revenue", revenue);
            categoryData.put("quantity", categoryQuantities.get(categoryId));

            // Calculate percentage
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = revenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP);
                categoryData.put("percentage", percentage);
            } else {
                categoryData.put("percentage", BigDecimal.ZERO);
            }

            result.add(categoryData);
        }

        // Sort by revenue descending
        result.sort((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")));

        return result;
    }

    /**
     * Create empty daily sales record
     */
    private DailySalesEntity createEmptyDailySales(UUID tenantId, LocalDate date) {
        DailySalesEntity sales = new DailySalesEntity();
        sales.setTenantId(tenantId);
        sales.setDate(date);
        sales.setOrderCount(0);
        sales.setProductCount(0);
        sales.setTotalSales(BigDecimal.ZERO);
        sales.setAverageOrderValue(BigDecimal.ZERO);
        sales.setDiscountsTotal(BigDecimal.ZERO);
        return sales;
    }

    /**
     * Get year-over-year comparison
     */
    public Map<String, Object> getYearOverYearComparison(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);

        // Current year data
        LocalDate currentYearStart = today.withDayOfYear(1);
        BigDecimal currentYearSales = dailySalesRepository
                .sumTotalSalesByTenantIdAndDateBetween(tenantId, currentYearStart, today);
        if (currentYearSales == null) {
            currentYearSales = BigDecimal.ZERO;
        }

        // Previous year data
        LocalDate previousYearStart = oneYearAgo.withDayOfYear(1);
        LocalDate previousYearEnd = oneYearAgo.withDayOfYear(oneYearAgo.lengthOfYear());
        BigDecimal previousYearSales = dailySalesRepository
                .sumTotalSalesByTenantIdAndDateBetween(tenantId, previousYearStart, previousYearEnd);
        if (previousYearSales == null) {
            previousYearSales = BigDecimal.ZERO;
        }

        // Calculate growth
        BigDecimal growth = BigDecimal.ZERO;
        if (previousYearSales.compareTo(BigDecimal.ZERO) > 0) {
            growth = currentYearSales.subtract(previousYearSales)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousYearSales, 2, RoundingMode.HALF_UP);
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("currentYearSales", currentYearSales);
        result.put("previousYearSales", previousYearSales);
        result.put("growth", growth);
        result.put("growthIsPositive", growth.compareTo(BigDecimal.ZERO) >= 0);

        return result;
    }

    /**
     * Get customer retention metrics
     */
    public Map<String, Object> getCustomerRetentionMetrics(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime endDate = today.atTime(23, 59, 59);
        LocalDateTime startDate30Days = today.minusDays(30).atStartOfDay();
        LocalDateTime startDate90Days = today.minusDays(90).atStartOfDay();

        // Get unique customers with orders in the last 30 days
        List<OrderEntity> orders30Days = orderRepository
                .findAllByTenantIdAndDateRangeAndDeletedFalse(tenantId, startDate30Days, endDate);

        Set<UUID> uniqueCustomers30Days = orders30Days.stream()
                .map(OrderEntity::getCustomerId)
                .collect(Collectors.toSet());

        // Get unique customers with orders in the last 90 days
        List<OrderEntity> orders90Days = orderRepository
                .findAllByTenantIdAndDateRangeAndDeletedFalse(tenantId, startDate90Days, endDate);

        Set<UUID> uniqueCustomers90Days = orders90Days.stream()
                .map(OrderEntity::getCustomerId)
                .collect(Collectors.toSet());

        // Get customers with repeat orders in the last 90 days
        Map<UUID, Long> customerOrderCounts = orders90Days.stream()
                .collect(Collectors.groupingBy(OrderEntity::getCustomerId, Collectors.counting()));

        long repeatCustomersCount = customerOrderCounts.values().stream()
                .filter(count -> count > 1)
                .count();

        // Calculate retention rate
        double retentionRate = 0.0;
        if (!uniqueCustomers90Days.isEmpty()) {
            retentionRate = (double) repeatCustomersCount / uniqueCustomers90Days.size() * 100;
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("uniqueCustomers30Days", uniqueCustomers30Days.size());
        result.put("uniqueCustomers90Days", uniqueCustomers90Days.size());
        result.put("repeatCustomers", repeatCustomersCount);
        result.put("retentionRate", Math.round(retentionRate * 100.0) / 100.0); // Round to 2 decimal places

        return result;
    }
}