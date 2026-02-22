package com.neighborshare.exception;

public class UnauthorizedException extends NeighborShareException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }
}
