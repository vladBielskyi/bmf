package ua.vbielskyi.bmf.core.event;

/**
 * Interface for event listeners
 * @param <T> Event type
 */
public interface EventListener<T> {

    /**
     * Handle an event
     * @param event The event to handle
     */
    void onEvent(T event);

    /**
     * Get event type this listener is interested in
     * @return Event type class
     */
    Class<T> getEventType();
}