package ua.vbielskyi.bmf.core.entity.bot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bot_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotSettingsEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "order_confirmation_message", columnDefinition = "TEXT")
    private String orderConfirmationMessage;

    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(name = "default_language")
    private String defaultLanguage;

    @Column(name = "enable_location_services")
    private boolean enableLocationServices;

    @Column(name = "enable_payments")
    private boolean enablePayments;

    @Column(name = "enable_web_app")
    private boolean enableWebApp;

    @Column(name = "web_app_url")
    private String webAppUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}