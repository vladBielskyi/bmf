package ua.vbielskyi.bmf.core.telegram.exception;

public class BotRegistryException extends RuntimeException {

    public BotRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public BotRegistryException(String message) {
        super(message);
    }
}
