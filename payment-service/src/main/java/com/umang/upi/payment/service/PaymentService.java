package com.umang.upi.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umang.upi.common.enums.PaymentStatus;
import com.umang.upi.common.event.PaymentInitiatedEvent;
import com.umang.upi.payment.entity.OutboxEvent;
import com.umang.upi.payment.entity.Payment;
import com.umang.upi.payment.repository.OutboxEventRepository;
import com.umang.upi.payment.repository.PaymentRepository;
import com.umang.upi.payment.web.dto.PaymentRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Payment initiatePayment(PaymentRequest request, String idempotencyKey) {
        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {} -> returning payment {}",
                    idempotencyKey, existing.get().getPaymentRef());
            return existing.get();
        }

        Instant now = Instant.now();
        String paymentRef = "PAY-" + UUID.randomUUID();
        Payment payment = Payment.builder()
                .paymentRef(paymentRef)
                .fromVpa(request.fromVpa())
                .toVpa(request.toVpa())
                .amount(request.amount())
                .status(PaymentStatus.INITIATED)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        payment = paymentRepository.save(payment);

        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                paymentRef, request.fromVpa(), request.toVpa(), request.amount(),
                UUID.randomUUID().toString(), now);
        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(paymentRef)
                .eventType(PaymentInitiatedEvent.class.getSimpleName())
                .payload(serialize(event))
                .published(false)
                .createdAt(now)
                .build());

        log.info("Initiated payment {} and wrote outbox event in one transaction", paymentRef);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getByRef(String paymentRef) {
        return paymentRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new IllegalArgumentException("No payment with ref " + paymentRef));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
