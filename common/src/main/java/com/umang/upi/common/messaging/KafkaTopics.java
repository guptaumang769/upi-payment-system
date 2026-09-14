package com.umang.upi.common.messaging;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String PAYMENT_INITIATED = "payment-initiated";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";
    public static final String WALLET_DEBITED = "wallet-debited";
    public static final String WALLET_DEBIT_FAILED = "wallet-debit-failed";

    public static final String DLT_SUFFIX = ".DLT";
}
