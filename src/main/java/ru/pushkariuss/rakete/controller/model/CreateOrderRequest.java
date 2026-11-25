package ru.pushkariuss.rakete.controller.model;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(Long userId, @Positive BigDecimal amount) {
}
