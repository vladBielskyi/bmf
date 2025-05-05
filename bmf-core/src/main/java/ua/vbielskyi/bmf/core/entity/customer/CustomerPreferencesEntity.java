package ua.vbielskyi.bmf.core.entity.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "preferred_language")
    private String preferredLanguage;

    @Column(name = "preferred_contact_method")
    private String preferredContactMethod;

    @Column(name = "preferred_delivery_time")
    private String preferredDeliveryTime;

    @Column(name = "favorite_product_ids", columnDefinition = "TEXT")
    private String favoriteProductIds;

    @Column(name = "special_occasions", columnDefinition = "TEXT")
    private String specialOccasions;

    @Column(name = "marketing_consent")
    private Boolean marketingConsent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}