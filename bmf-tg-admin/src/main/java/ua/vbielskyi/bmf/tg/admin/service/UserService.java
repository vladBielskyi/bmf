package ua.vbielskyi.bmf.tg.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.tg.admin.model.entity.User;
import ua.vbielskyi.bmf.tg.admin.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for user management operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Check if an email is already in use
     *
     * @param email Email to check
     * @return true if email is already in use
     */
    public boolean isEmailInUse(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Register a new user
     *
     * @param telegramId Telegram user ID
     * @param fullName User's full name
     * @param email User's email
     * @param phoneNumber User's phone number
     * @param preferredLanguage User's preferred language
     * @return The created user
     */
    @Transactional
    public User registerUser(Long telegramId, String fullName, String email,
                             String phoneNumber, String preferredLanguage) {
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByTelegramId(telegramId);

        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);
            user.setPreferredLanguage(preferredLanguage);
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            log.info("Updated existing user: {}", savedUser.getId());
            return savedUser;
        } else {
            // Create new user
            User newUser = User.builder()
                    .telegramId(telegramId)
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .preferredLanguage(preferredLanguage)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("Registered new user: {}", savedUser.getId());
            return savedUser;
        }
    }

    /**
     * Get a user by Telegram ID
     *
     * @param telegramId Telegram user ID
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    /**
     * Update user's language preference
     *
     * @param telegramId Telegram user ID
     * @param language Language code
     * @return true if update was successful
     */
    @Transactional
    public boolean updateUserLanguage(Long telegramId, String language) {
        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPreferredLanguage(language);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.debug("Updated language preference for user {}: {}", telegramId, language);
            return true;
        }

        return false;
    }

    /**
     * Check if a user exists
     *
     * @param telegramId Telegram user ID
     * @return true if user exists
     */
    public boolean userExists(Long telegramId) {
        return userRepository.existsByTelegramId(telegramId);
    }

    /**
     * Activate a user
     *
     * @param telegramId Telegram user ID
     * @return true if activation was successful
     */
    @Transactional
    public boolean activateUser(Long telegramId) {
        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Activated user: {}", telegramId);
            return true;
        }

        return false;
    }

    /**
     * Deactivate a user
     *
     * @param telegramId Telegram user ID
     * @return true if deactivation was successful
     */
    @Transactional
    public boolean deactivateUser(Long telegramId) {
        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Deactivated user: {}", telegramId);
            return true;
        }

        return false;
    }
}