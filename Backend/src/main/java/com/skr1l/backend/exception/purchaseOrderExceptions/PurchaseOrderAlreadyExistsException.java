package com.skr1l.backend.exception.purchaseOrderExceptions;

public class PurchaseOrderAlreadyExistsException extends RuntimeException {
    public PurchaseOrderAlreadyExistsException(String message) {
        super(message);
    }
}
