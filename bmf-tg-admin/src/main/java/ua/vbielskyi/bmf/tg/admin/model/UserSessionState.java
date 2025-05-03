package ua.vbielskyi.bmf.tg.admin.model;

/**
 * Represents all possible states of a user's session
 */
public enum UserSessionState {
    // Main states
    NEW,
    MAIN_MENU,
    SETTINGS_MENU,

    // Registration flow
    REGISTRATION_NAME,
    REGISTRATION_EMAIL,
    REGISTRATION_PHONE,
    REGISTRATION_LANGUAGE,
    REGISTRATION_CONFIRMATION,

    // Shop setup flow
    SHOP_SETUP_NAME,
    SHOP_SETUP_DESCRIPTION,
    SHOP_SETUP_BOT_TOKEN,
    SHOP_SETUP_BOT_USERNAME,
    SHOP_SETUP_SUBSCRIPTION,
    SHOP_SETUP_CONFIRMATION,

    // Shop management
    SHOP_MANAGEMENT,
    SHOP_PRODUCTS,
    SHOP_CATEGORIES,
    SHOP_ORDERS,
    SHOP_SETTINGS,

    // Product management
    PRODUCT_CREATE,
    PRODUCT_EDIT,
    PRODUCT_DELETE,

    // Category management
    CATEGORY_CREATE,
    CATEGORY_EDIT,
    CATEGORY_DELETE,

    // Order management
    ORDER_VIEW,
    ORDER_STATUS_UPDATE,

    // Settings
    PROFILE_SETTINGS,
    NOTIFICATION_SETTINGS,
    SUBSCRIPTION_SETTINGS,
    SECURITY_SETTINGS,
    LANGUAGE_SETTINGS
}