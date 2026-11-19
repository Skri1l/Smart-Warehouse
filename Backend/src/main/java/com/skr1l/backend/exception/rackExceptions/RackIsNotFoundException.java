package com.skr1l.backend.exception.rackExceptions;

public class RackIsNotFoundException extends RuntimeException {
    public RackIsNotFoundException(String message) {
        super(message);
    }
}
