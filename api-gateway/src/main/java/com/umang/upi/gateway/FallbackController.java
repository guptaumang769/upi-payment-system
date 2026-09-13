package com.umang.upi.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> paymentsFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"success\":false,\"error\":{\"code\":\"PAYMENT_SERVICE_UNAVAILABLE\","
                        + "\"message\":\"Payment service is temporarily unavailable, please retry.\"}}"));
    }
}
