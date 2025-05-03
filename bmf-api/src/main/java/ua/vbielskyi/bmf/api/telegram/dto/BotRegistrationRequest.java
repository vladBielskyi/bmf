package ua.vbielskyi.bmf.api.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotRegistrationRequest {

    /**
     * Telegram bot token
     */
    @NotBlank
    @Pattern(regexp = "^[0-9]{9}:[a-zA-Z0-9_-]{35}$", message = "Invalid bot token format")
    private String token;

    /**
     * Telegram bot username
     */
    @NotBlank
    private String username;

    /**
     * Tenant ID (null for admin bot)
     */
    private UUID tenantId;

    /**
     * Bot type (admin or tenant)
     */
    private String botType;
}
