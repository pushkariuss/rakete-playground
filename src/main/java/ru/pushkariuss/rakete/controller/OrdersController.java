package ru.pushkariuss.rakete.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pushkariuss.rakete.controller.model.CreateOrderRequest;
import ru.pushkariuss.rakete.controller.model.OrderResponse;
import ru.pushkariuss.rakete.model.Order;
import ru.pushkariuss.rakete.service.OrdersService;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        log.info("Got a create order request for user {} in thread {}", request.userId(), Thread.currentThread());
        final Order order = ordersService.createOrder(request);
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getAmount(),
                order.getStatus()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("id") UUID id) {
        log.info("Got a get order request for order {} in thread {}", id, Thread.currentThread());
        return ResponseEntity.of(ordersService.getOrder(id));
    }
}
