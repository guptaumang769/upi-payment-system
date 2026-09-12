package com.umang.upi.wallet.web;

import com.umang.upi.common.api.ApiResponse;
import com.umang.upi.common.api.ErrorResponse;
import com.umang.upi.wallet.exception.InsufficientBalanceException;
import com.umang.upi.wallet.exception.WalletNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(WalletNotFoundException ex) {
        return ApiResponse.failure(ErrorResponse.builder()
                .code("WALLET_NOT_FOUND").message(ex.getMessage()).build());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleInsufficient(InsufficientBalanceException ex) {
        return ApiResponse.failure(ErrorResponse.builder()
                .code("INSUFFICIENT_BALANCE").message(ex.getMessage()).build());
    }
}
