package ua.vbielskyi.bmf.core.entity.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(nullable = false)
    private String type;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_sent", nullable = false)
    private boolean sent;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}