package com.skr1l.backend.exception.shelfExceptions;

public class ShelfIsNotFoundException extends RuntimeException {
    public ShelfIsNotFoundException(String message) {
        super(message);
    }
}
