package com.umang.upi.payment.web.dto;

import com.umang.upi.common.enums.PaymentStatus;
import com.umang.upi.payment.entity.Payment;
import java.math.BigDecimal;

public record PaymentResponse(
        String paymentRef,
        String fromVpa,
        String toVpa,
        BigDecimal amount,
        PaymentStatus status) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getPaymentRef(), p.getFromVpa(), p.getToVpa(),
                p.getAmount(), p.getStatus());
    }
}
