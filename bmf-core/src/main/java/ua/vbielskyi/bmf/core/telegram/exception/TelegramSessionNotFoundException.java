package ua.vbielskyi.bmf.core.telegram.exception;

public class TelegramSessionNotFoundException extends RuntimeException {
    public TelegramSessionNotFoundException(String message) {
        super(message);
    }
}
