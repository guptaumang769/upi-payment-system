package com.umang.upi.payment.client;

import com.umang.upi.payment.client.dto.CreditRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/v1/wallets/{vpa}/credit")
    void credit(@PathVariable("vpa") String vpa, @RequestBody CreditRequest request);
}
