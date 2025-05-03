package ua.vbielskyi.bmf.tg.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.vbielskyi.bmf.tg.admin.model.entity.User;

import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by Telegram ID
     *
     * @param telegramId Telegram user ID
     * @return User with the given Telegram ID
     */
    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Check if a user with the given Telegram ID exists
     *
     * @param telegramId Telegram user ID
     * @return true if user exists
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Check if a user with the given email exists
     *
     * @param email Email to check
     * @return true if email is in use
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by email
     *
     * @param email Email to search for
     * @return User with the given email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by phone number
     *
     * @param phoneNumber Phone number to search for
     * @return User with the given phone number
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
}