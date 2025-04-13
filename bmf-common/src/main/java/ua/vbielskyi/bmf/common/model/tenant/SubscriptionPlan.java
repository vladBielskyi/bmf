package ua.vbielskyi.bmf.common.model.tenant;

import lombok.Getter;

/**
 * Represents subscription plans available for tenants
 */
@Getter
public enum SubscriptionPlan {
    FREE(100, 5, false, false, 1),
    BASIC(500, 20, true, false, 3),
    PREMIUM(2000, 100, true, true, 10),
    ENTERPRISE(10000, 500, true, true, 30);

    private final int maxProducts;
    private final int maxOrders;
    private final boolean customizationAllowed;
    private final boolean analyticsEnabled;
    private final int maxAdminUsers;

    SubscriptionPlan(int maxProducts, int maxOrders, boolean customizationAllowed,
                     boolean analyticsEnabled, int maxAdminUsers) {
        this.maxProducts = maxProducts;
        this.maxOrders = maxOrders;
        this.customizationAllowed = customizationAllowed;
        this.analyticsEnabled = analyticsEnabled;
        this.maxAdminUsers = maxAdminUsers;
    }

}