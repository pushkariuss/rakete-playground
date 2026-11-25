package ru.pushkariuss.rakete.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        Long userId,
        BigDecimal amount,
        Instant createdAt
) {}
