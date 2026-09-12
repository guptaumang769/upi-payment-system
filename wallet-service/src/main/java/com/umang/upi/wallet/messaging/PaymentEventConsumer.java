package com.umang.upi.wallet.messaging;

import com.umang.upi.common.event.PaymentInitiatedEvent;
import com.umang.upi.common.event.WalletDebitFailedEvent;
import com.umang.upi.common.event.WalletDebitedEvent;
import com.umang.upi.common.messaging.KafkaTopics;
import com.umang.upi.wallet.exception.InsufficientBalanceException;
import com.umang.upi.wallet.exception.WalletNotFoundException;
import com.umang.upi.wallet.service.WalletService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.PAYMENT_INITIATED)
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Received PaymentInitiatedEvent for payment {} ({} -> {})",
                event.paymentId(), event.fromVpa(), event.toVpa());
        try {
            walletService.debit(event.fromVpa(), event.amount(), event.paymentId());
            kafkaTemplate.send(KafkaTopics.WALLET_DEBITED, event.paymentId(),
                    new WalletDebitedEvent(event.paymentId(), event.fromVpa(), event.toVpa(),
                            event.amount(), event.correlationId(), Instant.now()));
            log.info("Debited payer for payment {}, published WalletDebitedEvent", event.paymentId());
        } catch (InsufficientBalanceException | WalletNotFoundException ex) {
            kafkaTemplate.send(KafkaTopics.WALLET_DEBIT_FAILED, event.paymentId(),
                    new WalletDebitFailedEvent(event.paymentId(), event.fromVpa(), event.toVpa(),
                            event.amount(), ex.getMessage(), event.correlationId(), Instant.now()));
            log.warn("Debit failed for payment {}: {}", event.paymentId(), ex.getMessage());
        }
    }
}
