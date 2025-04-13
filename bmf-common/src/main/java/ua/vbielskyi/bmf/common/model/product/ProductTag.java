package ua.vbielskyi.bmf.common.model.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a product tag
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTag {
    private UUID id;

    @NotNull
    private UUID tenantId;

    @NotBlank
    private String name;

    private String color;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}