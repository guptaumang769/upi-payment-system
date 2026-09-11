package com.umang.upi.payment.web;

import com.umang.upi.common.api.ApiResponse;
import com.umang.upi.payment.service.PaymentService;
import com.umang.upi.payment.web.dto.PaymentRequest;
import com.umang.upi.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<PaymentResponse> initiate(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey : UUID.randomUUID().toString();
        return ApiResponse.ok(PaymentResponse.from(paymentService.initiatePayment(request, key)));
    }

    @GetMapping("/{ref}")
    public ApiResponse<PaymentResponse> get(@PathVariable String ref) {
        return ApiResponse.ok(PaymentResponse.from(paymentService.getByRef(ref)));
    }
}
