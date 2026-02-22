package com.neighborshare.exception;

public class NeighborShareException extends RuntimeException {
    private final String errorCode;

    public NeighborShareException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public NeighborShareException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
