package com.umang.upi.wallet.repository;

import com.umang.upi.common.enums.TransactionType;
import com.umang.upi.wallet.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /** Idempotency probe: has this payment already produced a ledger entry of this type? */
    boolean existsByPaymentIdAndType(String paymentId, TransactionType type);
}
