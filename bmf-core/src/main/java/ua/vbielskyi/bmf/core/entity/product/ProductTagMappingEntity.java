package ua.vbielskyi.bmf.core.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "product_tag_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTagMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;
}