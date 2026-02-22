package com.neighborshare.exception;

public class BookingConflictException extends NeighborShareException {
    public BookingConflictException(String message) {
        super(message, "BOOKING_CONFLICT");
    }
}
