package com.skr1l.backend.exception.warehouseExceptions;

public class WarehouseAlreadyExistsException extends RuntimeException {
    public WarehouseAlreadyExistsException(String message) {
        super(message);
    }
}
