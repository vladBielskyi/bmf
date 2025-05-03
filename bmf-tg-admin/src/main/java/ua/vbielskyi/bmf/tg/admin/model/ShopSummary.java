package ua.vbielskyi.bmf.tg.admin.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for simplified tenant/shop information
 */
@Data
@Builder
public class ShopSummary {
    private UUID id;
    private String name;
    private String description;
    private String botUsername;
    private boolean active;
    private String subscriptionPlan;
    private LocalDateTime subscriptionExpiryDate;
    private String logoUrl;
    private int totalProducts;
    private int totalOrders;
}