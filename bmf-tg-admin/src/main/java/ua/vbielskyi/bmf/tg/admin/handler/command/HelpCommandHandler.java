package ua.vbielskyi.bmf.tg.admin.handler.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpCommandHandler implements CommandHandler {

    private final LocalizationService localizationService;

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        StringBuilder helpText = new StringBuilder();
        helpText.append(localizationService.getMessage("help.title", message.getUserId())).append("\n\n");

        // Commands section
        helpText.append("*").append(localizationService.getMessage("help.commands", message.getUserId())).append("*\n");
        helpText.append("/start - ").append(localizationService.getMessage("help.command.start", message.getUserId())).append("\n");
        helpText.append("/register - ").append(localizationService.getMessage("help.command.register", message.getUserId())).append("\n");
        helpText.append("/myshops - ").append(localizationService.getMessage("help.command.myshops", message.getUserId())).append("\n");
        helpText.append("/plans - ").append(localizationService.getMessage("help.command.plans", message.getUserId())).append("\n");
        helpText.append("/language - ").append(localizationService.getMessage("help.command.language", message.getUserId())).append("\n");
        helpText.append("/help - ").append(localizationService.getMessage("help.command.help", message.getUserId())).append("\n\n");

        // Support section
        helpText.append("*").append(localizationService.getMessage("help.support", message.getUserId())).append("*\n");
        helpText.append(localizationService.getMessage("help.support.text", message.getUserId())).append("\n\n");

        // Documentation section
        helpText.append("*").append(localizationService.getMessage("help.documentation", message.getUserId())).append("*\n");
        helpText.append(localizationService.getMessage("help.documentation.text", message.getUserId()));

        // Create buttons for support and documentation
        List<List<Object>> inlineKeyboard = new ArrayList<>();

        // Support button
        List<Object> row1 = new ArrayList<>();
        row1.add(createWebButton(
                "💬 " + localizationService.getMessage("button.support", message.getUserId()),
                "https://t.me/BotanicalMarketingFlowSupport"
        ));
        inlineKeyboard.add(row1);

        // Documentation button
        List<Object> row2 = new ArrayList<>();
        row2.add(createWebButton(
                "📚 " + localizationService.getMessage("button.documentation", message.getUserId()),
                "https://botanicalmarketingflow.com/docs"
        ));
        inlineKeyboard.add(row2);

       // return BotResponse.createWithInlineKeyboard(message.getChatId(), helpText.toString(), inlineKeyboard);
        return null;
    }

    private Object createWebButton(String text, String url) {
        return new Object[] { text, url };
    }
}