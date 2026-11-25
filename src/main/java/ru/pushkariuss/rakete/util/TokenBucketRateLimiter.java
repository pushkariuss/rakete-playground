package ru.pushkariuss.rakete.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter {

    private final long maxCapacity;
    private final double refillTokensPerNano;

    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long maxCapacity, long refillTokens, Duration refillPeriod) {
        if (maxCapacity <= 0 || refillTokens <= 0 || refillPeriod.isNegative() || refillPeriod.isZero()) {
            throw new IllegalArgumentException("Parameters must be positive");
        }
        this.maxCapacity = maxCapacity;
        this.refillTokensPerNano = (double) refillTokens / refillPeriod.toNanos();
    }

    public boolean tryConsume(String key) {
        final BucketState bucket = buckets.computeIfAbsent(key, k -> new BucketState(maxCapacity, System.nanoTime()));
        return bucket.tryConsume(1, maxCapacity, refillTokensPerNano);
    }

    private static class BucketState {
        private final ReentrantLock lock = new ReentrantLock();
        private double currentTokens;
        private long lastRefillTimestampNanos;

        BucketState(double initialTokens, long nowNanos) {
            this.currentTokens = initialTokens;
            this.lastRefillTimestampNanos = nowNanos;
        }

        boolean tryConsume(int tokensToConsume, long maxCapacity, double refillTokensPerNano) {
            lock.lock();
            try {
                final long now = System.nanoTime();
                final long elapsedNanos = Math.max(0, now - lastRefillTimestampNanos);
                final double tokensToAdd = elapsedNanos * refillTokensPerNano;

                if (tokensToAdd > 0) {
                    currentTokens = Math.min(maxCapacity, currentTokens + tokensToAdd);
                    lastRefillTimestampNanos = now;
                }

                if (currentTokens >= tokensToConsume) {
                    currentTokens -= tokensToConsume;
                    return true;
                }

                return false;
            } finally {
                lock.unlock();
            }
        }
    }
}
