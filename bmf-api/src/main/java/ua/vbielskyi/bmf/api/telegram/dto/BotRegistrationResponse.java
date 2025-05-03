package ua.vbielskyi.bmf.api.telegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotRegistrationResponse {

    /**
     * Whether the registration was successful
     */
    private boolean success;

    /**
     * Webhook URL if registration was successful
     */
    private String webhookUrl;

    /**
     * Error message if registration failed
     */
    private String errorMessage;
}
