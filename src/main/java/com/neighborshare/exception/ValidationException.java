package com.neighborshare.exception;

public class ValidationException extends NeighborShareException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}
