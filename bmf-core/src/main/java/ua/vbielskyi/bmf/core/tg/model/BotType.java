package ua.vbielskyi.bmf.core.tg.model;

import lombok.Getter;

/**
 * Enum representing the types of bots in the system
 */
@Getter
public enum BotType {
    /**
     * Admin bot for platform management
     */
    ADMIN("admin"),

    /**
     * Tenant bot for flower shops
     */
    TENANT("tenant"),
    /**
     * Driver bot for flower shops
     */
    DRIVER("driver");

    private final String value;

    BotType(String value) {
        this.value = value;
    }

    public static BotType fromValue(String value) {
        for (BotType botType : BotType.values()) {
            if (botType.value.equals(value)) {
                return botType;
            }
        }
        return null;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isTenant() {
        return this == TENANT;
    }
}