package ua.vbielskyi.bmf.core.entity.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.common.model.tenant.SubscriptionPlan;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "telegram_bot_token", nullable = false)
    private String telegramBotToken;

    @Column(name = "telegram_bot_username", nullable = false)
    private String telegramBotUsername;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "font_family")
    private String fontFamily;

    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "subscription_expiry_date")
    private LocalDateTime subscriptionExpiryDate;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}