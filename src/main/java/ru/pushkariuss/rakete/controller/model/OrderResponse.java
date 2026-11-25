package ru.pushkariuss.rakete.controller.model;

import ru.pushkariuss.rakete.model.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID id, Long userId, BigDecimal amount, OrderStatus status) {
}
