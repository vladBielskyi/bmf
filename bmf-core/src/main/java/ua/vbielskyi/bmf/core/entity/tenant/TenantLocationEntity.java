package ua.vbielskyi.bmf.core.entity.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.vbielskyi.bmf.core.tenant.entity.TenantAware;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantLocationEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String email;

    @Column(name = "working_hours", length = 1000)
    private String workingHours;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}