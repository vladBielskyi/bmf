package ua.vbielskyi.bmf.core.entity.loyalty;

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
@Table(name = "loyalty_programs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyProgramEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "points_per_currency", precision = 10, scale = 2)
    private BigDecimal pointsPerCurrency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}