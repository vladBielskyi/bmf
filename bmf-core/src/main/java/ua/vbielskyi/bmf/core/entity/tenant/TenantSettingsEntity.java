package ua.vbielskyi.bmf.core.entity.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingsEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "default_language", nullable = false)
    private String defaultLanguage;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "delivery_fee")
    private Double deliveryFee;

    @Column(name = "free_delivery_threshold")
    private Double freeDeliveryThreshold;

    @Column(name = "enable_payments")
    private boolean enablePayments;

    @Column(name = "allow_card_payments")
    private boolean allowCardPayments;

    @Column(name = "allow_cash_payments")
    private boolean allowCashPayments;

    @Column(name = "enable_location_services")
    private boolean enableLocationServices;

    @Column(name = "enable_notifications")
    private boolean enableNotifications;

    @Column(name = "allow_guest_checkout")
    private boolean allowGuestCheckout;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}