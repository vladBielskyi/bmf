package ua.vbielskyi.bmf.tg.admin.model;

import lombok.Data;
import ua.vbielskyi.bmf.common.model.tenant.Tenant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user's session state for the admin bot
 */
@Data
public class UserSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // User identification
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;

    // Session state
    private UserSessionState state;
    private LocalDateTime lastActivity;
    private String language;

    // Flow data
    private RegistrationData registrationData;
    private ShopSetupData shopSetupData;

    // Current context
    private Long currentShopId;
    private Tenant currentShop;

    // Additional data
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Check if this is a new user
     * @return true if this is a new session
     */
    public boolean isNewUser() {
        return registrationData == null || registrationData.isConfirmed() == null || !registrationData.isConfirmed();
    }

    /**
     * Check if user is in registration flow
     * @return true if in registration flow
     */
    public boolean isInRegistrationFlow() {
        return state != null && state.name().startsWith("REGISTRATION_");
    }

    /**
     * Check if user is in shop setup flow
     * @return true if in shop setup flow
     */
    public boolean isInShopSetupFlow() {
        return state != null && state.name().startsWith("SHOP_SETUP_");
    }

    /**
     * Add an attribute to the session
     * @param key Attribute key
     * @param value Attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Get an attribute from the session
     * @param key Attribute key
     * @param <T> Attribute type
     * @return Attribute value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * Remove an attribute from the session
     * @param key Attribute key
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Clear all attributes
     */
    public void clearAttributes() {
        attributes.clear();
    }
}