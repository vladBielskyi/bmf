package ua.vbielskyi.bmf.core.telegram.model;

/**
 * Enum representing different types of Telegram bot messages
 */
public enum BotMessageType {
    COMMAND,        // Bot commands (starting with /)
    TEXT,           // Regular text messages
    CALLBACK_QUERY, // Callback from inline keyboards
    WEBAPP_DATA,    // Data from Telegram WebApp
    LOCATION,       // Location data
    PHOTO,          // Photos
    DOCUMENT,       // Documents
    CONTACT,        // Contact information
    STICKER,        // Stickers
    VOICE,          // Voice messages
    UNKNOWN         // Unknown message type
}