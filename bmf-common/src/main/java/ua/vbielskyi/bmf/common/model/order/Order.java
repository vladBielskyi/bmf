package ua.vbielskyi.bmf.common.model.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a customer order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private UUID id;

    @NotNull
    private UUID tenantId;

    @NotNull
    private Long customerTelegramId;

    private String customerName;

    private String customerPhone;

    @NotNull
    private OrderStatus status;

    @NotEmpty
    private List<OrderItem> items;

    @NotNull
    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    @NotNull
    private BigDecimal finalAmount;

    private String deliveryAddress;

    private LocalDateTime deliveryTime;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private String paymentTransactionId;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deleted = false;
}