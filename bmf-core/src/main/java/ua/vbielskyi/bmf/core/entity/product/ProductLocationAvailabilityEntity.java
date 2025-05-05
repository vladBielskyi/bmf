package ua.vbielskyi.bmf.core.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_location_availability")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLocationAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}