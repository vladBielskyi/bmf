package ua.vbielskyi.bmf.core.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.customer.CustomerAddressEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, UUID> {

    List<CustomerAddressEntity> findAllByCustomerId(UUID customerId);

    Optional<CustomerAddressEntity> findByCustomerIdAndDefaultAddressTrue(UUID customerId);
}