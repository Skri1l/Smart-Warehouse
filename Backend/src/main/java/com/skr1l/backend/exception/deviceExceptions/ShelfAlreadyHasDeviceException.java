package com.skr1l.backend.exception.deviceExceptions;

public class ShelfAlreadyHasDeviceException extends RuntimeException {
    public ShelfAlreadyHasDeviceException(String message) {
        super(message);
    }
}
