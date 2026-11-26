package ru.pushkariuss.rakete.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.pushkariuss.rakete.model.OrderCreatedEvent;
import ru.pushkariuss.rakete.model.OrderStatus;
import ru.pushkariuss.rakete.repository.DeduplicationStatus;
import ru.pushkariuss.rakete.repository.OrderEventsDeduplicationRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final ObjectMapper objectMapper;
    private final OrderEventsDeduplicationRepository deduplicationRepository;
    private final OrdersService ordersService;

    @KafkaListener(topics = "${order.events.kafka.topic}")
    public void onMessage(String payload) {
        final OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        final UUID eventId = event.eventId();

        final DeduplicationStatus status = deduplicationRepository.getStatus(eventId);
        if (status == DeduplicationStatus.DONE) {
            log.info("Duplicate order event for id {}", eventId);
            return;
        }

        if (status == DeduplicationStatus.PROCESSING) {
            log.info("Order event for id {} is being processed by someone else or will be re-processed later", eventId);
            throw new RuntimeException("Event " + eventId + " being processed by someone else or will be reprocessed later");
        }

        final UUID orderId = event.orderId();
        try {
            log.info("Processing order event id {}, order {}", eventId, event.orderId());
            ordersService.markOrderProcessed(orderId);
        } catch (Exception e) {
            log.warn("Failed to update status of the order {} to {}", orderId, OrderStatus.PROCESSED);
            deduplicationRepository.delete(eventId);
            throw e;
        }

        deduplicationRepository.setDone(eventId);
        log.info("Done processing order event id {}, order {}", eventId, event.orderId());

        ordersService.refreshOrderCache(orderId);
    }

}
