package com.skr1l.backend.exception.deviceExceptions;

public class DeviceIsNotFoundException extends RuntimeException {
    public DeviceIsNotFoundException(String message) {
        super(message);
    }
}
