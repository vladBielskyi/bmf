package ua.vbielskyi.bmf.common.model.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a flower product that can be ordered
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private UUID id;

    @NotNull
    private UUID tenantId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @PositiveOrZero
    private BigDecimal discountPrice;

    private List<String> imageUrls;

    private String mainImageUrl;

    @NotNull
    private ProductCategory category;

    private List<ProductTag> tags;

    @PositiveOrZero
    private Integer availableStock;

    private boolean featured;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}