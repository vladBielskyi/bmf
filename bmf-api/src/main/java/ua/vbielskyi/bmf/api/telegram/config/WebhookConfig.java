package ua.vbielskyi.bmf.api.telegram.config;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationRequest;
import ua.vbielskyi.bmf.api.telegram.dto.BotRegistrationResponse;
import ua.vbielskyi.bmf.api.telegram.service.BotRegistrationApplicationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${bot.admin.token}")
    private String adminBotToken;

    @Value("${bot.admin.username}")
    private String adminBotUsername;

    private final BotRegistrationApplicationService registrationService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Setting up admin bot webhook");

        // Register admin bot
        BotRegistrationRequest request = new BotRegistrationRequest();
        request.setToken(adminBotToken);
        request.setUsername(adminBotUsername);
        request.setBotType("admin");

        BotRegistrationResponse response = registrationService.registerBot(request);

        if (response.isSuccess()) {
            log.info("Admin bot webhook set up successfully at {}", response.getWebhookUrl());
        } else {
            log.error("Failed to set up admin bot webhook: {}", response.getErrorMessage());
        }
    }
}