package ua.vbielskyi.bmf.tg.admin.handler.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.TenantManagementService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MyShopsCommandHandler implements CommandHandler {

    private final LocalizationService localizationService;
    private final TenantManagementService tenantService;

    @Override
    public String getCommand() {
        return "myshops";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        // Get user's shops
        List<TenantEntity> shops = tenantService.getUserShops(message.getUserId());

        if (shops.isEmpty()) {
            // No shops found - suggest registration
            String noShopsMessage = localizationService.getMessage("shops.none", message.getUserId());

            // Create inline keyboard with register button
            List<List<Object>> inlineKeyboard = new ArrayList<>();
            List<Object> row = new ArrayList<>();
            row.add(createInlineButton(
                    localizationService.getMessage("button.register_now", message.getUserId()),
                    "register_shop"
            ));
            inlineKeyboard.add(row);

            return BotResponse.createWithInlineKeyboard(message.getChatId(), noShopsMessage, inlineKeyboard);
        } else {
            // Show list of shops
            StringBuilder shopsMessage = new StringBuilder();
            shopsMessage.append(localizationService.getMessage("shops.list.title", message.getUserId())).append("\n\n");

            // Create inline keyboard with shop buttons
            List<List<Object>> inlineKeyboard = new ArrayList<>();

            for (TenantEntity shop : shops) {
                shopsMessage.append("🌸 *").append(shop.getShopName()).append("*\n");
                shopsMessage.append("  ").append(localizationService.getMessage("shops.status", message.getUserId())).append(": ");
                shopsMessage.append(shop.isActive() ? "✅" : "❌").append("\n");
                shopsMessage.append("  ").append(localizationService.getMessage("shops.subscription", message.getUserId())).append(": ");
                shopsMessage.append(shop.getSubscriptionPlan()).append("\n\n");

                // Add button for each shop
                List<Object> row = new ArrayList<>();
                row.add(createInlineButton(
                        "✏️ " + shop.getShopName(),
                        "shop_" + shop.getId()
                ));
                inlineKeyboard.add(row);
            }

            // Add row for registering a new shop
            List<Object> lastRow = new ArrayList<>();
            lastRow.add(createInlineButton(
                    "➕ " + localizationService.getMessage("button.add_new_shop", message.getUserId()),
                    "register_shop"
            ));
            inlineKeyboard.add(lastRow);

            return BotResponse.createWithInlineKeyboard(message.getChatId(), shopsMessage.toString(), inlineKeyboard);
        }
    }

    private Object createInlineButton(String text, String callbackData) {
        return new Object[] { text, callbackData };
    }
}