package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.webapp.AnswerWebAppQuery;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ua.vbielskyi.bmf.core.entity.bot.WebAppSettingsEntity;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.bot.WebAppSettingsRepository;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.BotExecutor;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramWebAppIntegrationService {

    private final WebAppSettingsRepository webAppSettingsRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final BotExecutor botExecutor;
    private final ua.vbielskyi.bmf.core.service.webapp.WebAppIntegrationService webAppIntegrationService;

    @Value("${telegram.payment.provider-token}")
    private String paymentProviderToken;

    @Value("${webapp.base-url}")
    private String webAppBaseUrl;

    /**
     * Send message with WebApp button
     */
    public void sendWebAppButton(UUID tenantId, Long chatId, String buttonText, String webAppPath) {
        try {
            // Get tenant
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

            // Create WebApp URL
            String webAppUrl = buildWebAppUrl(tenantId, webAppPath);

            // Create keyboard with WebApp button
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton webAppButton = new InlineKeyboardButton();
            webAppButton.setText(buttonText);
            WebAppInfo webAppInfo = new WebAppInfo();
            webAppInfo.setUrl(webAppUrl);
            webAppButton.setWebApp(webAppInfo);

            row.add(webAppButton);
            keyboard.add(row);
            keyboardMarkup.setKeyboard(keyboard);

            // Create message
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Open our catalog in Telegram WebApp:");
            message.setReplyMarkup(keyboardMarkup);

            // Execute message
            BotResponse response = BotResponse.builder()
                    .method(message)
                    .build();

            botExecutor.executeAsync(response, BotType.TENANT, tenantId);

            log.info("Sent WebApp button to chat {}, tenant {}", chatId, tenantId);
        } catch (Exception e) {
            log.error("Error sending WebApp button", e);
        }
    }

    /**
     * Respond to WebApp query
     */
    public void answerWebAppQuery(String queryId, UUID tenantId, String resultId, String title, String message) {
        try {
            AnswerWebAppQuery answer = new AnswerWebAppQuery();
            answer.setWebAppQueryId(queryId);
            // Set result - implementation depends on what the WebApp is sending

            // Execute response
            BotResponse response = BotResponse.builder()
                    .method(answer)
                    .build();

            botExecutor.execute(response, BotType.TENANT, tenantId);

            log.info("Answered WebApp query {}, tenant {}", queryId, tenantId);
        } catch (Exception e) {
            log.error("Error answering WebApp query", e);
        }
    }

    /**
     * Send invoice using Telegram Payments
     */
    public void sendInvoice(OrderEntity order) {
        try {
            UUID tenantId = order.getTenantId();
            Long chatId = order.getCustomerTelegramId();

            // Get tenant
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

            // Create prices list
            List<LabeledPrice> prices = new ArrayList<>();
            prices.add(new LabeledPrice(
                    "Order #" + order.getOrderNumber(),
                    order.getFinalAmount().multiply(BigDecimal.valueOf(100)).intValue() // Convert to cents
            ));

            // Create invoice
            SendInvoice invoice = new SendInvoice();
            invoice.setChatId(chatId.toString());
            invoice.setTitle("Order #" + order.getOrderNumber());
            invoice.setDescription("Payment for your order at " + tenant.getShopName());
            invoice.setPayload(order.getId().toString());
            invoice.setProviderToken(paymentProviderToken);
            invoice.setCurrency("USD"); // Should come from tenant settings
            invoice.setPrices(prices);
            invoice.setStartParameter("pay_" + order.getId());
            invoice.setPhotoUrl(getLogoUrl(tenantId));
            invoice.setNeedName(true);
            invoice.setNeedPhoneNumber(true);

            // Execute message
            BotResponse response = BotResponse.builder()
                    .method(invoice)
                    .build();

            botExecutor.executeAsync(response, BotType.TENANT, tenantId);

            log.info("Sent invoice for order {} to chat {}, tenant {}",
                    order.getId(), chatId, tenantId);
        } catch (Exception e) {
            log.error("Error sending invoice", e);
        }
    }

    /**
     * Create payment link
     */
    public String createPaymentLink(OrderEntity order) {
        try {
            UUID tenantId = order.getTenantId();

            // Get tenant
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

            // Create prices list
            List<LabeledPrice> prices = new ArrayList<>();
            prices.add(new LabeledPrice(
                    "Order #" + order.getOrderNumber(),
                    order.getFinalAmount().multiply(BigDecimal.valueOf(100)).intValue() // Convert to cents
            ));

            // Create invoice link
            CreateInvoiceLink invoiceLink = new CreateInvoiceLink();
            invoiceLink.setTitle("Order #" + order.getOrderNumber());
            invoiceLink.setDescription("Payment for your order at " + tenant.getShopName());
            invoiceLink.setPayload(order.getId().toString());
            invoiceLink.setProviderToken(paymentProviderToken);
            invoiceLink.setCurrency("USD"); // Should come from tenant settings
            invoiceLink.setPrices(prices);

            // For demonstration - in real implementation, this would be the actual API call
            // String paymentLink = botExecutor.execute(invoiceLink, BotType.TENANT, tenantId);
            String paymentLink = "https://t.me/sampleBot?start=pay_" + order.getId();

            log.info("Created payment link for order {}, tenant {}",
                    order.getId(), tenantId);

            return paymentLink;
        } catch (Exception e) {
            log.error("Error creating payment link", e);
            return null;
        }
    }

    /**
     * Build WebApp URL
     */
    private String buildWebAppUrl(UUID tenantId, String path) {
        // Generate auth token
        String authToken = webAppIntegrationService.generateAuthToken(tenantId, null);

        // Build WebApp URL
        return webAppBaseUrl +
                (path.startsWith("/") ? path : "/" + path) +
                "?tenantId=" + tenantId +
                "&auth=" + authToken;
    }

    /**
     * Get tenant logo URL
     */
    private String getLogoUrl(UUID tenantId) {
        Optional<WebAppSettingsEntity> settings = webAppSettingsRepository.findByTenantId(tenantId);
        if (settings.isPresent() && settings.get().getLogoUrl() != null) {
            return settings.get().getLogoUrl();
        }

        // Default logo
        return "https://example.com/default-logo.png";
    }
}