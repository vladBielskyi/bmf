package ua.vbielskyi.bmf.tg.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.cache.CacheService;
import ua.vbielskyi.bmf.tg.admin.model.RegistrationData;
import ua.vbielskyi.bmf.tg.admin.model.ShopSetupData;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;
import ua.vbielskyi.bmf.tg.admin.model.UserSessionState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user sessions
 */
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final String SESSION_KEY_PREFIX = "bmf:admin:session:";
    private static final Duration SESSION_EXPIRY = Duration.ofHours(24);

    private final CacheService cacheService;

    /**
     * Get or create a user session
     *
     * @param userId Telegram user ID
     * @return The user session
     */
    public UserSession getOrCreateSession(Long userId) {
        String key = getSessionKey(userId);
        Optional<UserSession> userSession = cacheService.get(key, UserSession.class);

        if (userSession.isPresent()) {
            UserSession session = userSession.get();
            session.setLastActivity(LocalDateTime.now());
            saveSession(session);
            return session;
        }

        // Create new session
        UserSession newSession = new UserSession();
        newSession.setUserId(userId);
        newSession.setState(UserSessionState.NEW);
        newSession.setLastActivity(LocalDateTime.now());

        saveSession(newSession);
        return newSession;
    }

    /**
     * Get a user session if it exists
     *
     * @param userId Telegram user ID
     * @return Optional containing the session or empty if not found
     */
    public Optional<UserSession> getSession(Long userId) {
        String key = getSessionKey(userId);
        Optional<UserSession> userSession = cacheService.get(key, UserSession.class);

        if (userSession.isPresent()) {
            UserSession session = userSession.get();
            session.setLastActivity(LocalDateTime.now());
            saveSession(session);
            return Optional.of(session);
        }

        return Optional.empty();
    }

    /**
     * Save a user session
     *
     * @param session The session to save
     */
    public void saveSession(UserSession session) {
        String key = getSessionKey(session.getUserId());
        session.setLastActivity(LocalDateTime.now());
        cacheService.put(key, session, SESSION_EXPIRY.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Delete a user session
     *
     * @param userId Telegram user ID
     */
    public void deleteSession(Long userId) {
        String key = getSessionKey(userId);
        cacheService.remove(key);
    }

    /**
     * Update a user's session state
     *
     * @param userId Telegram user ID
     * @param state New state
     */
    public void updateSessionState(Long userId, UserSessionState state) {
        UserSession session = getOrCreateSession(userId);
        session.setState(state);
        saveSession(session);
    }

    /**
     * Start the registration flow for a user
     *
     * @param userId Telegram user ID
     */
    public void startRegistrationFlow(Long userId) {
        UserSession session = getOrCreateSession(userId);
        session.setRegistrationData(new RegistrationData());
        session.setState(UserSessionState.REGISTRATION_NAME);
        saveSession(session);
    }

    /**
     * Start the shop setup flow for a user
     *
     * @param userId Telegram user ID
     */
    public void startShopSetupFlow(Long userId) {
        UserSession session = getOrCreateSession(userId);
        session.setShopSetupData(new ShopSetupData());
        session.setState(UserSessionState.SHOP_SETUP_NAME);
        saveSession(session);
    }

    /**
     * Reset a user to the main menu
     *
     * @param userId Telegram user ID
     */
    public void resetToMainMenu(Long userId) {
        UserSession session = getOrCreateSession(userId);
        session.setState(UserSessionState.MAIN_MENU);
        saveSession(session);
    }

    /**
     * Get the session key for Redis
     *
     * @param userId Telegram user ID
     * @return Redis key
     */
    private String getSessionKey(Long userId) {
        return SESSION_KEY_PREFIX + userId;
    }
}