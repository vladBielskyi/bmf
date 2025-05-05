package ua.vbielskyi.bmf.core.entity.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_sales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "product_count", nullable = false)
    private Integer productCount;

    @Column(name = "total_sales", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "average_order_value", precision = 10, scale = 2)
    private BigDecimal averageOrderValue;

    @Column(name = "discounts_total", precision = 10, scale = 2)
    private BigDecimal discountsTotal;
}