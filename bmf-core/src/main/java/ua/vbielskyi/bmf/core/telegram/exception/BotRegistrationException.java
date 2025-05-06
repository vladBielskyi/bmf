package ua.vbielskyi.bmf.core.telegram.exception;

public class BotRegistrationException extends RuntimeException {
    public BotRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BotRegistrationException(String message) {
        super(message);
    }
}
