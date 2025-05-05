package ua.vbielskyi.bmf.core.entity.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_performance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_sales", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "units_sold", nullable = false)
    private Integer unitsSold;

    @Column(name = "units_returned")
    private Integer unitsReturned;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}