package com.umang.upi.payment.client.dto;

import java.math.BigDecimal;

public record CreditRequest(BigDecimal amount, String paymentId) {
}
