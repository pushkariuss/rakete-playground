package ru.pushkariuss.rakete.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ExpiringCache<K, V> implements AutoCloseable {

    private record CacheEntry<V>(V value, Instant expiresAt) {
        public boolean isExpired(Instant now) {
            return expiresAt.isBefore(now);
        }
    }

    private record ExpirationTask<K, V>(K key, CacheEntry<V> entry) implements Delayed {

        @Override
        public int compareTo(Delayed o) {
            if (o == this) {
                return 0;
            }
            if (o instanceof ExpirationTask<?, ?>) {
                return entry.expiresAt().compareTo(((ExpirationTask<?, ?>) o).entry.expiresAt());
            }
            return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long remainingMillis = Duration.between(Instant.now(), entry.expiresAt()).toMillis();
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS);
        }
    }

    private Thread cleanupThread;

    private final Map<K, CacheEntry<V>> cache;
    private final DelayQueue<ExpirationTask<K, V>> queue;

    private ExpiringCache() {
        cache = new ConcurrentHashMap<>();
        queue = new DelayQueue<>();
    }

    public static <K, V> ExpiringCache<K, V> create() {
        ExpiringCache<K, V> expiringCache = new ExpiringCache<>();
        expiringCache.startCleanup();
        return expiringCache;
    }

    public void put(K key, V value, Duration ttl) {
        Objects.requireNonNull(key, "Key must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        Objects.requireNonNull(ttl, "TTL must not be null");
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }

        final Instant expiresAt = Instant.now().plus(ttl);
        final CacheEntry<V> entry = new CacheEntry<>(value, expiresAt);
        cache.put(key, entry);
        queue.offer(new ExpirationTask<>(key, entry));
    }

    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "Key must not be null");

        final Instant now = Instant.now();
        final CacheEntry<V> value = cache.computeIfPresent(key,
                (k, old) -> old.isExpired(now) ? null : old);
        return Optional.ofNullable(value).map(CacheEntry::value);
    }

    @Override
    public void close() throws Exception {
        final Thread thread = this.cleanupThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void cleanupJob() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ExpirationTask<K, V> expirationTask = queue.take();
                cache.remove(expirationTask.key(), expirationTask.entry());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startCleanup() {
        this.cleanupThread = Thread.startVirtualThread(this::cleanupJob);
    }

}
