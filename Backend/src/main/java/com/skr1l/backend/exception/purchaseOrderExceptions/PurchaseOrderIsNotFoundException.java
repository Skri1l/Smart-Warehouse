package com.skr1l.backend.exception.purchaseOrderExceptions;

public class PurchaseOrderIsNotFoundException extends RuntimeException {
    public PurchaseOrderIsNotFoundException(String message) {
        super(message);
    }
}
