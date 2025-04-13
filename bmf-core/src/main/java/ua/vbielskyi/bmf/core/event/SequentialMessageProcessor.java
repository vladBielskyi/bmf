package ua.vbielskyi.bmf.core.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Utility for processing messages that require sequential processing based on a key
 * Ensures messages for the same entity are processed in order but allows parallel processing
 * of messages for different entities
 */
@Component
@Slf4j
public class SequentialMessageProcessor {

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public <T> T process(String entityType, UUID entityId, Supplier<T> processor) {
        String lockKey = entityType + ":" + entityId;
        Lock lock = getLock(lockKey);

        try {
            lock.lock();
            return processor.get();
        } finally {
            lock.unlock();
            cleanupLock(lockKey);
        }
    }

    public void processAsync(String entityType, UUID entityId, Runnable processor) {
        String lockKey = entityType + ":" + entityId;
        Lock lock = getLock(lockKey);

        executor.submit(() -> {
            try {
                lock.lock();
                processor.run();
            } catch (Exception e) {
                log.error("Error processing message for {}:{}", entityType, entityId, e);
            } finally {
                lock.unlock();
                cleanupLock(lockKey);
            }
        });
    }

    private Lock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
    
    private void cleanupLock(String key) {
        Lock lock = locks.get(key);
        if (lock instanceof ReentrantLock && !((ReentrantLock) lock).isLocked()) {
            locks.remove(key);
        }
    }
}