package ua.vbielskyi.bmf.core.exception;

public class DuplicateTenantException extends RuntimeException {
    public DuplicateTenantException(String message) {
        super(message);
    }
}
