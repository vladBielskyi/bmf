package ua.vbielskyi.bmf.tg.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;
import ua.vbielskyi.bmf.core.entity.bot.TelegramSessionEntity;
import ua.vbielskyi.bmf.core.repository.bot.TelegramSessionRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing customer sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSessionService {

    private static final String SESSION_CACHE_KEY_FORMAT = "session:%d:%s";
    private static final long SESSION_CACHE_TTL = 30; // 30 minutes

    private final CacheService cacheService;
    private final TelegramSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get or create a customer session
     *
     * @param telegramId Telegram user ID
     * @param tenantId Tenant ID
     * @return Customer session
     */
    public CustomerSession getOrCreateSession(Long telegramId, UUID tenantId) {
        String cacheKey = String.format(SESSION_CACHE_KEY_FORMAT, telegramId, tenantId);

        // Try to get session from cache
        CustomerSession session = cacheService.getOrElseCompute(
                cacheKey,
                CustomerSession.class,
                () -> loadOrCreateSessionFromDatabase(telegramId, tenantId),
                SESSION_CACHE_TTL,
                TimeUnit.MINUTES,
                tenantId);

        return session;
    }

    /**
     * Save a customer session
     *
     * @param session Session to save
     */
    public void saveSession(CustomerSession session) {
        String cacheKey = String.format(SESSION_CACHE_KEY_FORMAT,
                session.getTelegramId(), session.getTenantId());

        // Update session in cache
        cacheService.put(cacheKey, session, SESSION_CACHE_TTL, TimeUnit.MINUTES, session.getTenantId());

        // Save to database asynchronously
        saveSessionToDatabase(session);
    }

    /**
     * Load or create a session from the database
     */
    private CustomerSession loadOrCreateSessionFromDatabase(Long telegramId, UUID tenantId) {
        try {
            // Try to find existing session
            TelegramSessionEntity entity = sessionRepository.findByTenantIdAndTelegramId(tenantId, telegramId)
                    .orElse(null);

            if (entity != null) {
                // Deserialize session data
                CustomerSession session = objectMapper.readValue(entity.getSessionData(), CustomerSession.class);
                session.setLastActivity(LocalDateTime.now());
                return session;
            }

            // Create new session
            CustomerSession session = new CustomerSession();
            session.setTelegramId(telegramId);
            session.setTenantId(tenantId);
            session.setState("MAIN_MENU");
            session.setLastActivity(LocalDateTime.now());
            session.setCreatedAt(LocalDateTime.now());

            return session;
        } catch (Exception e) {
            log.error("Error loading session from database", e);

            // Fallback to a new session
            CustomerSession session = new CustomerSession();
            session.setTelegramId(telegramId);
            session.setTenantId(tenantId);
            session.setState("MAIN_MENU");
            session.setLastActivity(LocalDateTime.now());
            session.setCreatedAt(LocalDateTime.now());

            return session;
        }
    }

    /**
     * Save session to database
     */
    private void saveSessionToDatabase(CustomerSession session) {
        try {
            // Find existing entity or create new one
            TelegramSessionEntity entity = sessionRepository.findByTenantIdAndTelegramId(
                            session.getTenantId(), session.getTelegramId())
                    .orElse(new TelegramSessionEntity());

            // Update entity fields
            entity.setTenantId(session.getTenantId());
            entity.setTelegramId(session.getTelegramId());
            entity.setCurrentState(session.getState());
            entity.setLanguageCode(session.getLanguage());
            entity.setLastActivityAt(session.getLastActivity());
            entity.setSessionData(objectMapper.writeValueAsString(session));

            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }

            entity.setUpdatedAt(LocalDateTime.now());

            // Save entity
            sessionRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("Error serializing session data", e);
        } catch (Exception e) {
            log.error("Error saving session to database", e);
        }
    }

    /**
     * Customer session data
     */
    @Setter
    @Getter
    public static class CustomerSession {
        private Long telegramId;
        private UUID tenantId;
        private String state;
        private String language;
        private UUID currentCategoryId;
        private UUID currentProductId;
        private UUID currentOrderId;
        private LocalDateTime lastActivity;
        private LocalDateTime createdAt;

    }
}