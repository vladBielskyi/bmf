package ua.vbielskyi.bmf.core.entity.loyalty;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_tiers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "required_points", nullable = false)
    private int requiredPoints;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "bonus_points_multiplier")
    private int bonusPointsMultiplier;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}