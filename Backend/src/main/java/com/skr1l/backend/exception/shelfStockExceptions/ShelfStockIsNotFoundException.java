package com.skr1l.backend.exception.shelfStockExceptions;

public class ShelfStockIsNotFoundException extends RuntimeException {
    public ShelfStockIsNotFoundException(String message) {
        super(message);
    }
}
