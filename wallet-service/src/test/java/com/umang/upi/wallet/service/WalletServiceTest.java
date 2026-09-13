package com.umang.upi.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umang.upi.common.enums.TransactionType;
import com.umang.upi.wallet.entity.LedgerEntry;
import com.umang.upi.wallet.entity.Wallet;
import com.umang.upi.wallet.exception.InsufficientBalanceException;
import com.umang.upi.wallet.repository.LedgerEntryRepository;
import com.umang.upi.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private LedgerEntryRepository ledgerRepository;
    @InjectMocks
    private WalletService walletService;

    private Wallet wallet(String vpa, String balance) {
        return Wallet.builder().id(1L).vpa(vpa).balance(new BigDecimal(balance)).version(0L).build();
    }

    @Test
    void debit_reducesBalance_andAppendsLedgerEntry() {
        Wallet w = wallet("alice@upi", "100.00");
        when(walletRepository.findByVpa("alice@upi")).thenReturn(Optional.of(w));
        when(ledgerRepository.existsByPaymentIdAndType("p1", TransactionType.DEBIT)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.debit("alice@upi", new BigDecimal("40.00"), "p1");

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    void debit_isIdempotent_whenLedgerEntryAlreadyExists() {
        Wallet w = wallet("alice@upi", "100.00");
        when(walletRepository.findByVpa("alice@upi")).thenReturn(Optional.of(w));
        when(ledgerRepository.existsByPaymentIdAndType("p1", TransactionType.DEBIT)).thenReturn(true);

        Wallet result = walletService.debit("alice@upi", new BigDecimal("40.00"), "p1");

        assertThat(result.getBalance()).isEqualByComparingTo("100.00");
        verify(ledgerRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void debit_throws_whenInsufficientBalance() {
        Wallet w = wallet("bob@upi", "10.00");
        when(walletRepository.findByVpa("bob@upi")).thenReturn(Optional.of(w));
        when(ledgerRepository.existsByPaymentIdAndType(eq("p2"), eq(TransactionType.DEBIT)))
                .thenReturn(false);

        assertThatThrownBy(() -> walletService.debit("bob@upi", new BigDecimal("50.00"), "p2"))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void credit_increasesBalance() {
        Wallet w = wallet("carol@upi", "0.00");
        when(walletRepository.findByVpa("carol@upi")).thenReturn(Optional.of(w));
        when(ledgerRepository.existsByPaymentIdAndType("p3", TransactionType.CREDIT)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.credit("carol@upi", new BigDecimal("75.00"), "p3");

        assertThat(result.getBalance()).isEqualByComparingTo("75.00");
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }
}
