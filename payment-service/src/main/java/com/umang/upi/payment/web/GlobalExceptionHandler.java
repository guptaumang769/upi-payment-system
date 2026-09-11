package com.umang.upi.payment.web;

import com.umang.upi.common.api.ApiResponse;
import com.umang.upi.common.api.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(IllegalArgumentException ex) {
        return ApiResponse.failure(ErrorResponse.builder()
                .code("PAYMENT_NOT_FOUND").message(ex.getMessage()).build());
    }
}
