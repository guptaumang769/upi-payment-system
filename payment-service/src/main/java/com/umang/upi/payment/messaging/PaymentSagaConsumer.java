package com.umang.upi.payment.messaging;

import com.umang.upi.common.enums.PaymentStatus;
import com.umang.upi.common.event.WalletDebitFailedEvent;
import com.umang.upi.common.event.WalletDebitedEvent;
import com.umang.upi.common.messaging.KafkaTopics;
import com.umang.upi.payment.client.WalletGateway;
import com.umang.upi.payment.entity.Payment;
import com.umang.upi.payment.repository.PaymentRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaConsumer {

    private final PaymentRepository paymentRepository;
    private final WalletGateway walletGateway;

    @KafkaListener(topics = KafkaTopics.WALLET_DEBITED,
            containerFactory = "walletDebitedListenerFactory")
    @Transactional
    public void onWalletDebited(WalletDebitedEvent event) {
        log.info("Payer debited for payment {} -> crediting payee {}",
                event.paymentId(), event.toVpa());
        walletGateway.credit(event.toVpa(), event.amount(), event.paymentId());
        updateStatus(event.paymentId(), PaymentStatus.COMPLETED);
    }

    @KafkaListener(topics = KafkaTopics.WALLET_DEBIT_FAILED,
            containerFactory = "walletDebitFailedListenerFactory")
    @Transactional
    public void onWalletDebitFailed(WalletDebitFailedEvent event) {
        log.warn("Debit failed for payment {} ({}) -> compensating: mark FAILED",
                event.paymentId(), event.reason());
        updateStatus(event.paymentId(), PaymentStatus.FAILED);
    }

    private void updateStatus(String paymentRef, PaymentStatus status) {
        Payment payment = paymentRepository.findByPaymentRef(paymentRef).orElse(null);
        if (payment == null) {
            log.error("Saga event for unknown payment ref {}", paymentRef);
            return;
        }
        payment.setStatus(status);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
    }
}
