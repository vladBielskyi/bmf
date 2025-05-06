package ua.vbielskyi.bmf.core.entity.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.common.model.order.PaymentMethod;
import ua.vbielskyi.bmf.common.model.order.PaymentStatus;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE orders SET is_deleted = true, updated_at = now() WHERE id = ?")
@Where(clause = "is_deleted = false")
public class OrderEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_telegram_id", nullable = false)
    private Long customerTelegramId;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Pre-persist hook to set createdAt and updatedAt before initial persist
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = OrderStatus.NEW;
        }

        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    /**
     * Pre-update hook to set updatedAt before each update
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate the total amount based on the items
     * @param items Order items
     */
    public void calculateTotals(List<OrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            this.finalAmount = BigDecimal.ZERO;
            return;
        }

        this.totalAmount = items.stream()
                .map(OrderItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }

        this.finalAmount = this.totalAmount.subtract(this.discountAmount);

        // Ensure final amount is never negative
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Check if an order status transition is valid
     * @param newStatus The new status
     * @return true if the transition is valid
     */
    public boolean isValidStatusTransition(OrderStatus newStatus) {
        if (newStatus == null || this.status == null) {
            return false;
        }

        // Allow if the status is not changing
        if (this.status == newStatus) {
            return true;
        }

        // Define valid transitions
        switch (this.status) {
            case NEW:
                return newStatus == OrderStatus.CONFIRMED ||
                        newStatus == OrderStatus.CANCELLED;

            case CONFIRMED:
                return newStatus == OrderStatus.PROCESSING ||
                        newStatus == OrderStatus.CANCELLED;

            case PROCESSING:
                return newStatus == OrderStatus.READY_FOR_DELIVERY ||
                        newStatus == OrderStatus.CANCELLED;

            case READY_FOR_DELIVERY:
                return newStatus == OrderStatus.OUT_FOR_DELIVERY ||
                        newStatus == OrderStatus.CANCELLED;

            case OUT_FOR_DELIVERY:
                return newStatus == OrderStatus.DELIVERED ||
                        newStatus == OrderStatus.CANCELLED;

            case DELIVERED:
                return newStatus == OrderStatus.COMPLETED ||
                        newStatus == OrderStatus.REFUNDED;

            case COMPLETED:
                return newStatus == OrderStatus.REFUNDED;

            case CANCELLED:
                return newStatus == OrderStatus.REFUNDED;

            case REFUNDED:
                return false; // No valid transitions from REFUNDED

            default:
                return false;
        }
    }
}