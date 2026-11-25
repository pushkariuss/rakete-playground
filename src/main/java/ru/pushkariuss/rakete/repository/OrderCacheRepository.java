package ru.pushkariuss.rakete.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.pushkariuss.rakete.controller.model.OrderResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheRepository {

    private static final String PREFIX = "order:";

    @Value("${order.events.redis.deduplication.ttl}")
    private Duration redisOrderEventKeyTtl;
    @Value("${order.response.redis.cache.ttl}")
    private Duration redisOrderCacheKeyTtl;
    @Value("${order.response.redis.cache.ttl.jitter}")
    private Duration redisOrderCacheKeyTtlJitter;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<OrderResponse> findById(UUID orderId) {
        final String value = redisTemplate.opsForValue().get(orderId.toString());
        if(value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, OrderResponse.class));
        } catch (JacksonException e) {
           log.warn("Cannot deserialize cache value for order {}", orderId);
            return Optional.empty();
        }
    }

    public void save(OrderResponse order) {
        try {
            final String value =  objectMapper.writeValueAsString(order);

            final long upperBound = redisOrderCacheKeyTtlJitter.toMillis();
            final long jitterMillis = ThreadLocalRandom.current().nextLong(upperBound);
            final Duration ttlWithJitter = redisOrderCacheKeyTtl.plusMillis(jitterMillis);

            redisTemplate.opsForValue().set(PREFIX + order.id(), value, ttlWithJitter);
        } catch (JacksonException e) {
            log.error("Cannot serialize cache value for order {}", order.id());
        }
    }
}
