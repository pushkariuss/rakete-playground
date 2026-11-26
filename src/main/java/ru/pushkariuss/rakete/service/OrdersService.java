package ru.pushkariuss.rakete.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pushkariuss.rakete.controller.model.CreateOrderRequest;
import ru.pushkariuss.rakete.controller.model.OrderResponse;
import ru.pushkariuss.rakete.model.Order;
import ru.pushkariuss.rakete.model.OrderCreatedEvent;
import ru.pushkariuss.rakete.model.OrderStatus;
import ru.pushkariuss.rakete.model.Outbox;
import ru.pushkariuss.rakete.repository.OrderCacheRepository;
import ru.pushkariuss.rakete.repository.OrdersRepository;
import ru.pushkariuss.rakete.repository.OutboxRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OrderCacheRepository orderCacheRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order o = new Order();
        o.setUserId(request.userId());
        o.setAmount(request.amount());

        final Order savedOrder = ordersRepository.save(o);

        final OrderCreatedEvent orderCreatedEvent = createOrderEvent(savedOrder);
        final Outbox outbox = new Outbox();
        outbox.setAggregateId(savedOrder.getId());
        outbox.setPayload(objectMapper.writeValueAsString(orderCreatedEvent));
        final Outbox savedOutbox = outboxRepository.save(outbox);

        log.info("Saved order {} for user {}, saved an outbox event {}",
                savedOrder.getId(), savedOrder.getUserId(), savedOutbox.getId());
        return savedOrder;
    }

    @Transactional
    public void markOrderProcessed(UUID orderId) {
        log.info("Marking order {} as {}", orderId, OrderStatus.PROCESSED);
        ordersRepository.markProcessed(orderId);
    }

    public Optional<OrderResponse> getOrder(UUID orderId) {
        return orderCacheRepository.findById(orderId)
                .or(() -> {
                    log.info("Cache miss for order {}, trying database...", orderId);

                    Optional<OrderResponse> orderResponse = ordersRepository.findById(orderId)
                            .map(this::createOrderResponse);
                    orderResponse.ifPresentOrElse(o -> {
                        log.info("Found an order {}, saving in cache", o.id());
                        orderCacheRepository.save(o);
                    }, () -> log.info("Not found an order {} in database", orderId));
                    return orderResponse;
                });
    }

    private OrderCreatedEvent createOrderEvent(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getUserId(),
                order.getAmount(),
                order.getCreatedAt()
        );
    }

    private OrderResponse createOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getAmount(),
                order.getStatus()
        );
    }
}
