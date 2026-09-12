package com.umang.upi.wallet.repository;

import com.umang.upi.wallet.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByVpa(String vpa);
}
