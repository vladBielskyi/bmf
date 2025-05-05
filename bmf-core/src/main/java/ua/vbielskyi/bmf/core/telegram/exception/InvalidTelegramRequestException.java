package ua.vbielskyi.bmf.core.telegram.exception;

public class InvalidTelegramRequestException extends RuntimeException {
    public InvalidTelegramRequestException(String message) {
        super(message);
    }
}
