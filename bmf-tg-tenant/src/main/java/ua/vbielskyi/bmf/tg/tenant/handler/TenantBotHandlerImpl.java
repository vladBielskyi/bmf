package ua.vbielskyi.bmf.tg.tenant.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.telegram.BotRegistry;
import ua.vbielskyi.bmf.core.telegram.handler.CallbackQueryHandler;
import ua.vbielskyi.bmf.core.telegram.handler.CommandHandler;
import ua.vbielskyi.bmf.core.telegram.handler.WebAppDataHandler;
import ua.vbielskyi.bmf.core.telegram.handler.impl.AbstractBotHandler;
import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
import ua.vbielskyi.bmf.core.telegram.model.BotType;
import ua.vbielskyi.bmf.core.telegram.service.impl.CachedBotRegistry;
import ua.vbielskyi.bmf.tg.tenant.service.CustomerSessionService;
import ua.vbielskyi.bmf.tg.tenant.service.CustomerSessionService.CustomerSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handler for tenant bots with improved error handling, session management,
 * and customer registration
 */
@Slf4j
@Service
public class TenantBotHandlerImpl extends AbstractBotHandler {

    private final BotRegistry botRegistry;
    private final CachedBotRegistry cachedBotRegistry;
    private final CustomerSessionService sessionService;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public TenantBotHandlerImpl(BotRegistry botRegistry,
                                CachedBotRegistry cachedBotRegistry,
                                CustomerSessionService sessionService,
                                CustomerRepository customerRepository,
                                TenantRepository tenantRepository,
                                List<CommandHandler> commandHandlers,
                                List<CallbackQueryHandler> callbackHandlers,
                                List<WebAppDataHandler> webAppHandlers) {
        super(commandHandlers, callbackHandlers, webAppHandlers);
        this.botRegistry = botRegistry;
        this.cachedBotRegistry = cachedBotRegistry;
        this.sessionService = sessionService;
        this.customerRepository = customerRepository;
        this.tenantRepository = tenantRepository;
    }

    @PostConstruct
    public void init() {
        botRegistry.registerHandler(this);
        log.info("Registered tenant bot handler");
    }

    @Override
    public boolean canHandle(BotType botType, UUID tenantId) {
        return botType == BotType.TENANT && tenantId != null;
    }

    @Override
    public BotType getBotType() {
        return BotType.TENANT;
    }

    @Override
    protected BotResponse handleText(BotMessage message) {
        // Verify tenant exists and is active
        CachedBotRegistry.BotConfig config = cachedBotRegistry.getBotConfig(BotType.TENANT, message.getTenantId());
        if (config == null || !config.isActive()) {
            log.error("No active tenant configuration found for: {}", message.getTenantId());
            return BotResponse.text(message.getChatId(), "This bot is currently inactive. Please try again later.");
        }

        // Get tenant information
        Optional<TenantEntity> tenantOpt = tenantRepository.findById(message.getTenantId());
        if (tenantOpt.isEmpty()) {
            log.error("Tenant not found in database: {}", message.getTenantId());
            return BotResponse.text(message.getChatId(), "Sorry, there was a problem with this flower shop's account. Please try again later.");
        }

        TenantEntity tenant = tenantOpt.get();

        // Check if customer exists, if not register them
        CustomerEntity customer = getOrCreateCustomer(message.getUserId(), message.getTenantId());

        // Get or create session
        CustomerSession session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());
        session.setLastActivity(LocalDateTime.now());

        BotResponse response;

        // Handle message based on current session state
        switch (session.getState()) {
            case "CATALOG_BROWSING":
                response = handleCatalogBrowsing(message, tenant, customer);
                break;

            case "ORDER_CREATION":
                response = handleOrderCreation(message, tenant, customer);
                break;

            case "CHECKOUT":
                response = handleCheckout(message, tenant, customer);
                break;

            case "WAITING_FOR_PHONE":
                response = handlePhoneNumber(message, customer);
                break;

            case "MAIN_MENU":
            default:
                response = handleMainMenu(message, tenant, customer);
                break;
        }

        // Save the updated session
        sessionService.saveSession(session);

        return response;
    }

    private BotResponse handleMainMenu(BotMessage message, TenantEntity tenant, CustomerEntity customer) {
        // Create main menu keyboard
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .keyboard(createMainMenuKeyboard())
                .build();

        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("Welcome to " + tenant.getShopName() + "! How can I help you today?")
                .replyMarkup(keyboardMarkup)
                .build();

        return BotResponse.builder()
                .method(sendMessage)
                .build();
    }

    private List<KeyboardRow> createMainMenuKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🌹 Browse Catalog"));
        row1.add(new KeyboardButton("🛒 My Cart"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📦 My Orders"));
        row2.add(new KeyboardButton("❓ Help"));

        KeyboardRow row3 = new KeyboardRow();
        KeyboardButton phoneButton = new KeyboardButton("📞 Share Phone Number");
        phoneButton.setRequestContact(true);
        row3.add(phoneButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return keyboard;
    }

    private BotResponse handleCatalogBrowsing(BotMessage message, TenantEntity tenant, CustomerEntity customer) {
        // Implementation for catalog browsing
        return BotResponse.text(message.getChatId(),
                "You are browsing our catalog. Here are our popular categories:\n\n" +
                        "1. Roses\n2. Tulips\n3. Mixed Bouquets\n4. Special Occasions\n\n" +
                        "Reply with a number to see products in that category.");
    }

    private BotResponse handleOrderCreation(BotMessage message, TenantEntity tenant, CustomerEntity customer) {
        // Implementation for order creation
        return BotResponse.text(message.getChatId(),
                "You are creating an order. Please follow the instructions or use /cancel to abort.");
    }

    private BotResponse handleCheckout(BotMessage message, TenantEntity tenant, CustomerEntity customer) {
        // Implementation for checkout process
        return BotResponse.text(message.getChatId(),
                "You are in the checkout process. Please follow the instructions or use /cancel to abort.");
    }

    private BotResponse handlePhoneNumber(BotMessage message, CustomerEntity customer) {
        // Check if message contains phone number
        String text = message.getText();

        if (text != null && text.matches("^\\+?[0-9\\s-()]{10,15}$")) {
            // Valid phone number format
            customer.setPhone(text.replaceAll("[\\s-()]", ""));
            customerRepository.save(customer);

            // Update session state
            CustomerSession session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());
            session.setState("MAIN_MENU");
            sessionService.saveSession(session);

            return BotResponse.text(message.getChatId(),
                    "Thank you! Your phone number has been saved. You can now proceed with shopping.");
        } else {
            return BotResponse.text(message.getChatId(),
                    "Please enter a valid phone number or use the 'Share Phone Number' button.");
        }
    }

    @Override
    protected BotResponse handleOtherMessageTypes(BotMessage message) {
        switch (message.getType()) {
            case LOCATION:
                return handleLocation(message);

            case CONTACT:
                return handleContact(message);

            case PHOTO:
                return BotResponse.text(message.getChatId(),
                        "Thank you for the photo. Our team will review it.");

            default:
                return BotResponse.text(message.getChatId(),
                        "This message type is not supported by our flower shop bot. " +
                                "Please use text messages or commands.");
        }
    }

    private BotResponse handleLocation(BotMessage message) {
        // Store location for delivery or show nearby shops
        CustomerSession session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());

        if ("WAITING_FOR_DELIVERY_LOCATION".equals(session.getState())) {
            session.setState("CHECKOUT");
            sessionService.saveSession(session);

            return BotResponse.text(message.getChatId(),
                    "Thank you for sharing your delivery location. We'll use it for your order.");
        } else {
            return BotResponse.text(message.getChatId(),
                    "Thank you for sharing your location. We'll show nearby flower shops soon.");
        }
    }

    private BotResponse handleContact(BotMessage message) {
        // Update customer phone number from shared contact
        if (message.getUpdate().getMessage().getContact() != null) {
            String phoneNumber = message.getUpdate().getMessage().getContact().getPhoneNumber();

            CustomerEntity customer = getOrCreateCustomer(message.getUserId(), message.getTenantId());
            customer.setPhone(phoneNumber);
            customerRepository.save(customer);

            // Update session state
            CustomerSession session = sessionService.getOrCreateSession(message.getUserId(), message.getTenantId());
            session.setState("MAIN_MENU");
            sessionService.saveSession(session);

            return BotResponse.text(message.getChatId(),
                    "Thank you! Your contact information has been saved. You can now proceed with shopping.");
        } else {
            return BotResponse.text(message.getChatId(),
                    "Sorry, we couldn't process your contact information. Please try again.");
        }
    }

    @Override
    protected boolean isAuthenticated(BotMessage message) {
        // For tenant bots, all users are considered authenticated
        // Additional logic could be added here for specific authentication requirements
        return true;
    }

    /**
     * Get or create a customer for the given user and tenant
     */
    private CustomerEntity getOrCreateCustomer(Long telegramId, UUID tenantId) {
        Optional<CustomerEntity> customerOpt = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId);

        if (customerOpt.isPresent()) {
            return customerOpt.get();
        } else {
            // Create new customer
            CustomerEntity customer = new CustomerEntity();
            customer.setTenantId(tenantId);
            customer.setTelegramId(telegramId);
            customer.setActive(true);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());

            return customerRepository.save(customer);
        }
    }
}