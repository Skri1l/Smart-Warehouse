package com.skr1l.backend.exception.productExceptions;

public class ProductIsNotFoundException extends RuntimeException {
    public ProductIsNotFoundException(String message) {
        super(message);
    }
}
