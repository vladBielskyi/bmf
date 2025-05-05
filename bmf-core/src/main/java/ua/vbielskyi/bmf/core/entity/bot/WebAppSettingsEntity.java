package ua.vbielskyi.bmf.core.entity.bot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "web_app_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAppSettingsEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "accent_color")
    private String accentColor;

    @Column(name = "font_family")
    private String fontFamily;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "enable_cart")
    private boolean enableCart;

    @Column(name = "enable_favorites")
    private boolean enableFavorites;

    @Column(name = "enable_search")
    private boolean enableSearch;

    @Column(name = "enable_filters")
    private boolean enableFilters;

    @Column(name = "products_per_page")
    private Integer productsPerPage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}