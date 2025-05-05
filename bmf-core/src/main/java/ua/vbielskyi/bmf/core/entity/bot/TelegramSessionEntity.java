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
@Table(name = "telegram_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSessionEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "session_data", columnDefinition = "TEXT")
    private String sessionData;

    @Column(name = "current_state")
    private String currentState;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}