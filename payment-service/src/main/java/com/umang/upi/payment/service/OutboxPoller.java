package com.umang.upi.payment.service;

import com.umang.upi.common.messaging.KafkaTopics;
import com.umang.upi.payment.entity.OutboxEvent;
import com.umang.upi.payment.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPoller(OutboxEventRepository outboxRepository,
                        KafkaTemplate<String, String> kafkaTemplate,
                        @Value("${payment.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending =
                outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(topicFor(event.getEventType()), event.getAggregateId(),
                        event.getPayload()).get();
                event.setPublished(true);
            } catch (ExecutionException | InterruptedException e) {
                log.error("Failed to publish outbox event {} — will retry next poll", event.getId(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                break;
            }
        }
        outboxRepository.saveAll(pending);
        log.info("Published {} outbox event(s) to Kafka", pending.size());
    }

    private String topicFor(String eventType) {
        return KafkaTopics.PAYMENT_INITIATED;
    }
}
