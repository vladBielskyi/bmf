package ua.vbielskyi.bmf.core.entity.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
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

    @Column(name = "is_default")
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}