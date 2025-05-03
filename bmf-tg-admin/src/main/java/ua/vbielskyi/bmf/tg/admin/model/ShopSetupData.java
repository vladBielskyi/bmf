package ua.vbielskyi.bmf.tg.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Data class to hold shop setup information during the setup flow
 */
@Data
public class ShopSetupData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shopName;
    private String description;
    private String botToken;
    private String botUsername;
    private String subscriptionPlan;
    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;

    // Flag to indicate if this is an update to an existing shop
    private boolean update;

    // ID of existing shop if this is an update
    private Long existingShopId;
}