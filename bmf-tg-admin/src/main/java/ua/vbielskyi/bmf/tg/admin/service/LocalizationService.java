package ua.vbielskyi.bmf.tg.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.tg.admin.model.UserSession;

import java.util.Locale;

/**
 * Service for handling localized messages
 */
@Service
@RequiredArgsConstructor
public class LocalizationService {

    private final MessageSource messageSource;
    private final AdminSessionService sessionService;

    /**
     * Get a localized message for the user
     *
     * @param key Message key
     * @param userId User ID
     * @param args Message arguments
     * @return Localized message
     */
    public String getMessage(String key, Long userId, Object... args) {
        UserSession session = sessionService.getOrCreateSession(userId);
        Locale locale = getUserLocale(session);
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Get a localized message for the user
     *
     * @param key Message key
     * @param session User session
     * @param args Message arguments
     * @return Localized message
     */
    public String getMessage(String key, UserSession session, Object... args) {
        Locale locale = getUserLocale(session);
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Get the user's preferred locale
     *
     * @param session User session
     * @return User's locale
     */
    public Locale getUserLocale(UserSession session) {
        String language = session.getLanguage();
        if (language == null || language.isEmpty()) {
            return Locale.ENGLISH; // Default
        }
        return Locale.forLanguageTag(language);
    }

    /**
     * Set the user's preferred language
     *
     * @param userId User ID
     * @param languageCode Language code (e.g., "en", "uk", "ru")
     */
    public void setUserLanguage(Long userId, String languageCode) {
        UserSession session = sessionService.getOrCreateSession(userId);
        session.setLanguage(languageCode);
        sessionService.saveSession(session);
    }

    /**
     * Get a list of supported languages
     *
     * @return Array of supported language codes
     */
    public String[] getSupportedLanguages() {
        return new String[]{"en", "uk", "ru"};
    }

    /**
     * Get a localized language name
     *
     * @param languageCode Language code
     * @param userLocale User's locale for the name to be displayed in
     * @return Localized language name
     */
    public String getLanguageName(String languageCode, Locale userLocale) {
        return messageSource.getMessage("language." + languageCode, null, userLocale);
    }
}