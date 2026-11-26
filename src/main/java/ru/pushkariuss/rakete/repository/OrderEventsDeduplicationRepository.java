package ru.pushkariuss.rakete.repository;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.pushkariuss.rakete.model.OrderStatus;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderEventsDeduplicationRepository {

    private static final String PREFIX = "order:event:dedup:";
    private static final String PROCESSING = "PROCESSING";

    @Value("${order.events.redis.deduplication.ttl}")
    private Duration dedupTtl;
    @Value("${order.events.redis.deduplication.processing.ttl}")
    private Duration dedupProcessingTtl;

    private final StringRedisTemplate redisTemplate;

    public DeduplicationStatus getStatus(UUID eventId) {
        final String key = generateKey(eventId);
        final boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(key, DeduplicationStatus.PROCESSING.toString(), dedupProcessingTtl);
        if (isNew) {
            return DeduplicationStatus.NEW;
        } else {
            final String notNewValue = redisTemplate.opsForValue().get(key);
            if (Objects.equals(notNewValue, DeduplicationStatus.DONE.toString())) {
                return DeduplicationStatus.DONE;
            } else {
                return DeduplicationStatus.PROCESSING;
            }
        }
    }

    public void delete(UUID eventId) {
        redisTemplate.opsForValue().getAndDelete(generateKey(eventId));

    }

    public void setDone(UUID eventId) {
        redisTemplate.opsForValue()
                .setIfPresent(generateKey(eventId), DeduplicationStatus.DONE.toString(), dedupTtl);
    }

    private String generateKey(UUID eventId) {
        return PREFIX + eventId;
    }
}
