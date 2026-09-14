package com.umang.upi.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFailedEvent(
        String paymentId,
        String fromVpa,
        String toVpa,
        BigDecimal amount,
        String reason,
        String correlationId,
        Instant timestamp) {
}
