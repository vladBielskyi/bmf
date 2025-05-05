package ua.vbielskyi.bmf.core.repository.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    List<TenantEntity> findAllByActiveTrue();

    Optional<TenantEntity> findByTelegramBotUsername(String botUsername);

    boolean existsByTelegramBotToken(String token);

    boolean existsByTelegramBotUsername(String username);
}