package com.demo.firma.exception;

public class DomainException extends RuntimeException {
    private final int status;
    public DomainException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
