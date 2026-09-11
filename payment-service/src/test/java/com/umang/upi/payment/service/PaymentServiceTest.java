package com.umang.upi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umang.upi.common.enums.PaymentStatus;
import com.umang.upi.payment.entity.OutboxEvent;
import com.umang.upi.payment.entity.Payment;
import com.umang.upi.payment.repository.OutboxEventRepository;
import com.umang.upi.payment.repository.PaymentRepository;
import com.umang.upi.payment.web.dto.PaymentRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private PaymentService paymentService;

    private PaymentService service() {
        return new PaymentService(paymentRepository, outboxRepository, objectMapper);
    }

    @Test
    void initiate_createsPayment_andWritesOutboxEvent() {
        paymentService = service();
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRequest req = new PaymentRequest("alice@upi", "bob@upi", new BigDecimal("100.00"));
        Payment result = paymentService.initiatePayment(req, "key-1");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.getIdempotencyKey()).isEqualTo("key-1");
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void initiate_isIdempotent_returnsExistingOnReplayedKey() {
        paymentService = service();
        Payment existing = Payment.builder()
                .paymentRef("PAY-existing").idempotencyKey("key-1")
                .status(PaymentStatus.INITIATED).build();
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        PaymentRequest req = new PaymentRequest("alice@upi", "bob@upi", new BigDecimal("100.00"));
        Payment result = paymentService.initiatePayment(req, "key-1");

        assertThat(result.getPaymentRef()).isEqualTo("PAY-existing");
        verify(paymentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }
}
