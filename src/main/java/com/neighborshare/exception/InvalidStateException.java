package com.neighborshare.exception;

public class InvalidStateException extends NeighborShareException {
    public InvalidStateException(String message) {
        super(message, "INVALID_STATE");
    }
}
