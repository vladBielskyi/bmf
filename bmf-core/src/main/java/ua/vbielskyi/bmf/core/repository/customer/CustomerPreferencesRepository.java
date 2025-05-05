package ua.vbielskyi.bmf.core.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.customer.CustomerPreferencesEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPreferencesRepository extends JpaRepository<CustomerPreferencesEntity, UUID> {

    Optional<CustomerPreferencesEntity> findByCustomerId(UUID customerId);
}