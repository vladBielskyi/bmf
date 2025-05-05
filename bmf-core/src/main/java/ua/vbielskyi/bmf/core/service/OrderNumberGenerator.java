package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.cache.CacheService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique order numbers for each tenant
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final String ORDER_COUNTER_KEY_PREFIX = "order:counter:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final CacheService cacheService;

    /**
     * Generate a unique order number for the tenant
     * Format: {YY}{MM}{DD}-{TENANT_PREFIX}-{COUNTER}
     */
    public String generateOrderNumber(UUID tenantId) {
        String datePrefix = LocalDateTime.now().format(DATE_FORMAT);
        String tenantPrefix = tenantId.toString().substring(0, 4).toUpperCase();

        // Get and increment counter
        String counterKey = ORDER_COUNTER_KEY_PREFIX + tenantId + ":" + datePrefix;

        AtomicInteger counter = cacheService.getOrElseCompute(
                counterKey,
                AtomicInteger.class,
                () -> new AtomicInteger(1),
                24,
                TimeUnit.HOURS,
                tenantId);

        int orderNumber = counter.getAndIncrement();

        // Update counter in cache
        cacheService.put(counterKey, counter, 24, TimeUnit.HOURS, tenantId);

        return String.format("%s-%s-%05d", datePrefix, tenantPrefix, orderNumber);
    }
}