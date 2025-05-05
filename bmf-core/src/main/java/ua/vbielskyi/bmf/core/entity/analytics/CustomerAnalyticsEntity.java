package ua.vbielskyi.bmf.core.entity.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "total_spent", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "average_order_value", precision = 10, scale = 2)
    private BigDecimal averageOrderValue;

    @Column(name = "first_purchase_date")
    private LocalDateTime firstPurchaseDate;

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;

    @Column(name = "favorite_category_id")
    private UUID favoriteCategoryId;

    @Column(name = "favorite_category_name")
    private String favoriteCategoryName;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;
}