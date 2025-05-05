package ua.vbielskyi.bmf.core.entity.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "previous_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

    @Column(name = "new_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus newStatus;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}