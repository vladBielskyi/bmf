package ua.vbielskyi.bmf.tg.admin.handler.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.common.model.tenant.SubscriptionPlan;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionPlansCommandHandler implements CommandHandler {

    private final LocalizationService localizationService;

    @Override
    public String getCommand() {
        return "plans";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        StringBuilder plansMessage = new StringBuilder();
        plansMessage.append(localizationService.getMessage("subscription.plans.title", message.getUserId())).append("\n\n");

        // Show available subscription plans with features
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            plansMessage.append("*").append(plan.name()).append("*\n");
            plansMessage.append("📱 ").append(localizationService.getMessage("subscription.max_products", message.getUserId())).append(": ").append(plan.getMaxProducts()).append("\n");
            plansMessage.append("📦 ").append(localizationService.getMessage("subscription.max_orders", message.getUserId())).append(": ").append(plan.getMaxOrders()).append("\n");
            plansMessage.append("👥 ").append(localizationService.getMessage("subscription.max_users", message.getUserId())).append(": ").append(plan.getMaxAdminUsers()).append("\n");
            plansMessage.append("🎨 ").append(localizationService.getMessage("subscription.customization", message.getUserId())).append(": ").append(plan.isCustomizationAllowed() ? "✅" : "❌").append("\n");
            plansMessage.append("📊 ").append(localizationService.getMessage("subscription.analytics", message.getUserId())).append(": ").append(plan.isAnalyticsEnabled() ? "✅" : "❌").append("\n\n");
        }

        // Add subscription management info
        plansMessage.append(localizationService.getMessage("subscription.plans.more_info", message.getUserId()));

        // Create inline keyboard with web button to subscription page
        List<List<Object>> inlineKeyboard = new ArrayList<>();
        List<Object> row = new ArrayList<>();

        // Web button to subscription page
        row.add(createWebButton(
                localizationService.getMessage("button.subscribe", message.getUserId()),
                "https://botanicalmarketingflow.com/subscription"
        ));

        inlineKeyboard.add(row);

        return null;
       // return BotResponse.createWithInlineKeyboard(message.getChatId(), plansMessage.toString(), inlineKeyboard);
    }

    private Object createWebButton(String text, String url) {
        return new Object[] { text, url };
    }
}