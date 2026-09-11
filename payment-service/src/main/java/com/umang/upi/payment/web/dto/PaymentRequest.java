package com.umang.upi.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String fromVpa,
        @NotBlank String toVpa,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
