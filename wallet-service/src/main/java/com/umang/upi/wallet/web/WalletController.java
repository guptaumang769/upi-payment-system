package com.umang.upi.wallet.web;

import com.umang.upi.common.api.ApiResponse;
import com.umang.upi.wallet.service.WalletService;
import com.umang.upi.wallet.web.dto.AmountRequest;
import com.umang.upi.wallet.web.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{vpa}")
    public ApiResponse<WalletResponse> balance(@PathVariable String vpa) {
        return ApiResponse.ok(WalletResponse.from(walletService.getWallet(vpa)));
    }

    @PostMapping("/{vpa}/debit")
    public ApiResponse<WalletResponse> debit(@PathVariable String vpa,
                                             @Valid @RequestBody AmountRequest request) {
        return ApiResponse.ok(
                WalletResponse.from(walletService.debit(vpa, request.amount(), request.paymentId())));
    }

    @PostMapping("/{vpa}/credit")
    public ApiResponse<WalletResponse> credit(@PathVariable String vpa,
                                              @Valid @RequestBody AmountRequest request) {
        return ApiResponse.ok(
                WalletResponse.from(walletService.credit(vpa, request.amount(), request.paymentId())));
    }
}
