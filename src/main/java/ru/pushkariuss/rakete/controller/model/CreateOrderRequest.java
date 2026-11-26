package ru.pushkariuss.rakete.controller.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(@NotNull Long userId,
                                 @NotNull @Positive BigDecimal amount) {
}
