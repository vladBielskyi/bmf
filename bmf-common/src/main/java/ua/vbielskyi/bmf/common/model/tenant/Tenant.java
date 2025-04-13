package ua.vbielskyi.bmf.common.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a tenant (flower shop) in the system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String shopName;

    private String description;

    private String logoUrl;

    @NotBlank
    @Pattern(regexp = "^[0-9]{9}:[a-zA-Z0-9_-]{35}$", message = "Invalid Telegram bot token format")
    private String telegramBotToken;

    @NotBlank
    private String telegramBotUsername;

    private String primaryColor;

    private String secondaryColor;

    private String fontFamily;

    private SubscriptionPlan subscriptionPlan;

    private LocalDateTime subscriptionExpiryDate;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}