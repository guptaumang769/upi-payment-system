package com.umang.upi.wallet.web.dto;

import com.umang.upi.wallet.entity.Wallet;
import java.math.BigDecimal;

public record WalletResponse(String vpa, BigDecimal balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getVpa(), wallet.getBalance());
    }
}
