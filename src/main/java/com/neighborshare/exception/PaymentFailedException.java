package com.neighborshare.exception;

public class PaymentFailedException extends NeighborShareException {
    public PaymentFailedException(String message) {
        super(message, "PAYMENT_FAILED");
    }
}
