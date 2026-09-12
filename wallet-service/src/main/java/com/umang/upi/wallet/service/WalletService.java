package com.umang.upi.wallet.service;

import com.umang.upi.common.enums.TransactionType;
import com.umang.upi.wallet.entity.LedgerEntry;
import com.umang.upi.wallet.entity.Wallet;
import com.umang.upi.wallet.exception.InsufficientBalanceException;
import com.umang.upi.wallet.exception.WalletNotFoundException;
import com.umang.upi.wallet.repository.LedgerEntryRepository;
import com.umang.upi.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;

    @Transactional
    public Wallet debit(String vpa, BigDecimal amount, String paymentId) {
        Wallet wallet = requireWallet(vpa);

        if (ledgerRepository.existsByPaymentIdAndType(paymentId, TransactionType.DEBIT)) {
            log.info("Idempotent replay: debit for payment {} already applied, skipping", paymentId);
            return wallet;
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in wallet " + vpa + " for amount " + amount);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        appendLedger(wallet, TransactionType.DEBIT, amount, newBalance, paymentId);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet credit(String vpa, BigDecimal amount, String paymentId) {
        Wallet wallet = requireWallet(vpa);

        if (ledgerRepository.existsByPaymentIdAndType(paymentId, TransactionType.CREDIT)) {
            log.info("Idempotent replay: credit for payment {} already applied, skipping", paymentId);
            return wallet;
        }

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        appendLedger(wallet, TransactionType.CREDIT, amount, newBalance, paymentId);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(String vpa) {
        return requireWallet(vpa);
    }

    private Wallet requireWallet(String vpa) {
        return walletRepository.findByVpa(vpa)
                .orElseThrow(() -> new WalletNotFoundException("No wallet for VPA " + vpa));
    }

    private void appendLedger(Wallet wallet, TransactionType type, BigDecimal amount,
                              BigDecimal balanceAfter, String paymentId) {
        ledgerRepository.save(LedgerEntry.builder()
                .walletId(wallet.getId())
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .paymentId(paymentId)
                .createdAt(Instant.now())
                .build());
    }
}
