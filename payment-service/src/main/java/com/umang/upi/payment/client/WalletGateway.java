package com.umang.upi.payment.client;

import com.umang.upi.payment.client.dto.CreditRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletGateway {

    private final WalletClient walletClient;

    @CircuitBreaker(name = "walletService", fallbackMethod = "creditFallback")
    @Retry(name = "walletService")
    @Bulkhead(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Void> credit(String vpa, BigDecimal amount, String paymentId) {
        return CompletableFuture.supplyAsync(() -> {
            walletClient.credit(vpa, new CreditRequest(amount, paymentId));
            return null;
        });
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Void> creditFallback(String vpa, BigDecimal amount,
                                                   String paymentId, Throwable t) {
        log.warn("Credit to {} for payment {} fell back (wallet-service unavailable): {}",
                vpa, paymentId, t.toString());
        return CompletableFuture.completedFuture(null);
    }
}
