package ua.vbielskyi.bmf.core.event;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Collection;

/**
 * Base interface for Redis-based event listeners
 */
public interface RedisEventListener extends MessageListener {

    /**
     * Get channels this listener is interested in
     * @return Collection of channel topics
     */
    Collection<ChannelTopic> getChannels();

    /**
     * Register this listener with the Redis message listener container
     * @param container Redis message listener container
     */
    default void register(RedisMessageListenerContainer container) {
        container.addMessageListener(this, getChannels());
    }

    /**
     * Unregister this listener from the Redis message listener container
     * @param container Redis message listener container
     */
    default void unregister(RedisMessageListenerContainer container) {
        container.removeMessageListener(this);
    }
}