package ua.vbielskyi.bmf.core.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTagEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column
    private String color;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}