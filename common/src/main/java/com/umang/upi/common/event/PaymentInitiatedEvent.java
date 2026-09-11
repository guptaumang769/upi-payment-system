package com.umang.upi.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentInitiatedEvent(
        String paymentId,
        String fromVpa,
        String toVpa,
        BigDecimal amount,
        String correlationId,
        Instant timestamp) {
}
