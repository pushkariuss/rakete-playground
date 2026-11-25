package ru.pushkariuss.rakete.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pushkariuss.rakete.model.Outbox;
import ru.pushkariuss.rakete.model.OutboxStatus;
import ru.pushkariuss.rakete.repository.OutboxRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingPublisher {

    @Value("${outbox.polling.publisher.batch.size:50}")
    private int batchSize;
    @Value("${order.events.kafka.topic}")
    private String ordersTopic;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @Scheduled(fixedRateString = "${outbox.polling.publisher.fixed.rate}")
    public void pollAndPublishOutbox() {
        final List<Outbox> newOutboxRecords = outboxRepository.getNewOutboxRecords(batchSize);
        log.info("Got {} outbox messages in thread {}", newOutboxRecords.size(), Thread.currentThread());

        for (Outbox outbox : newOutboxRecords) {
            try {
                log.info("Sending event for order {} to Kafka", outbox.getAggregateId());

                final String key = outbox.getAggregateId().toString();
                final String value = outbox.getPayload();
                kafkaTemplate.send(ordersTopic, key, value).get(5, TimeUnit.SECONDS);

                outbox.setStatus(OutboxStatus.SENT);
            } catch (Exception e) {
                log.error("Could not send message id={} to Kafka", outbox.getId());
                break;
            }
        }
    }
}
