package ua.vbielskyi.bmf.tg.admin.handler.callback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.telegram.handler.CallbackQueryHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;
import ua.vbielskyi.bmf.tg.admin.service.LocalizationService;
import ua.vbielskyi.bmf.tg.admin.service.TenantManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShopDetailsCallbackHandler implements CallbackQueryHandler {

    private final LocalizationService localizationService;
    private final TenantManagementService tenantService;
    private final CachedBotRegistry botRegistry;

    @Override
    public String getCallbackPrefix() {
        return "shop_";
    }

    @Override
    public BotResponse handle(BotMessage message) {
        String callbackData = message.getCallbackData();
        String shopIdStr = callbackData.substring(5); // Remove "shop_" prefix
        UUID shopId = UUID.fromString(shopIdStr);

        // Get shop details
        TenantEntity shop = tenantService.getShopById(shopId);
        if (shop == null) {
            return BotResponse.text(message.getChatId(),
                    localizationService.getMessage("shops.not_found", message.getUserId()));
        }

        // Get bot status from registry
        CachedBotRegistry.BotConfig botConfig = botRegistry.getBotConfig(BotType.TENANT, shopId);
        boolean botActive = botConfig != null && botConfig.isActive();

        // Build detailed shop info
        StringBuilder shopInfo = new StringBuilder();
        shopInfo.append("🌸 *").append(shop.getShopName()).append("*\n\n");

        // Shop details
        shopInfo.append("📝 ").append(localizationService.getMessage("shops.description", message.getUserId())).append(": ");
        shopInfo.append(shop.getDescription() != null ? shop.getDescription() : "-").append("\n\n");

        shopInfo.append("📊 ").append(localizationService.getMessage("shops.status", message.getUserId())).append(": ");
        shopInfo.append(shop.getActive() ? "✅" : "❌").append("\n");

        shopInfo.append("🤖 ").append(localizationService.getMessage("shops.bot_status", message.getUserId())).append(": ");
        shopInfo.append(botActive ? "✅" : "❌").append("\n");

        shopInfo.append("🔄 ").append(localizationService.getMessage("shops.subscription", message.getUserId())).append(": ");
        shopInfo.append(shop.getSubscriptionPlan()).append("\n");

        if (shop.getSubscriptionExpiryDate() != null) {
            shopInfo.append("📅 ").append(localizationService.getMessage("shops.expiry_date", message.getUserId())).append(": ");
            shopInfo.append(shop.getSubscriptionExpiryDate().toLocalDate()).append("\n\n");
        }

        // Bot info
        shopInfo.append("*").append(localizationService.getMessage("shops.bot_info", message.getUserId())).append("*\n");
        shopInfo.append("👤 @").append(shop.getTelegramBotUsername()).append("\n\n");

        // Create action buttons
        List<List<Object>> inlineKeyboard = new ArrayList<>();

        // Row 1 - Web admin & Activate/Deactivate bot
        List<Object> row1 = new ArrayList<>();
        row1.add(createWebButton(
                "🖥 " + localizationService.getMessage("button.web_admin", message.getUserId()),
                "https://botanicalmarketingflow.com/admin?tenant=" + shopId
        ));

        String botActionText = botActive ?
                "🔴 " + localizationService.getMessage("button.deactivate_bot", message.getUserId()) :
                "🟢 " + localizationService.getMessage("button.activate_bot", message.getUserId());
        row1.add(createInlineButton(botActionText, "bot_toggle_" + shopId));

        inlineKeyboard.add(row1);

        // Row 2 - Orders & Products
        List<Object> row2 = new ArrayList<>();
        row2.add(createInlineButton(
                "📦 " + localizationService.getMessage("button.orders", message.getUserId()),
                "orders_" + shopId
        ));
        row2.add(createInlineButton(
                "🌹 " + localizationService.getMessage("button.products", message.getUserId()),
                "products_" + shopId
        ));
        inlineKeyboard.add(row2);

        // Row 3 - Edit & Back
        List<Object> row3 = new ArrayList<>();
        row3.add(createInlineButton(
                "✏️ " + localizationService.getMessage("button.edit_shop", message.getUserId()),
                "edit_shop_" + shopId
        ));
        row3.add(createInlineButton(
                "⬅️ " + localizationService.getMessage("button.back", message.getUserId()),
                "back_to_shops"
        ));
        inlineKeyboard.add(row3);

       // return BotResponse.createWithInlineKeyboard(message.getChatId(), shopInfo.toString(), inlineKeyboard);
        return null;
    }

    private Object createInlineButton(String text, String callbackData) {
        return new Object[] { text, callbackData };
    }

    private Object createWebButton(String text, String url) {
        return new Object[] { text, url };
    }
}