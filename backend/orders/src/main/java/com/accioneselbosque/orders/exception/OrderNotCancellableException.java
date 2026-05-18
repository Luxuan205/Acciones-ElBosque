package com.accioneselbosque.orders.exception;

public class OrderNotCancellableException extends RuntimeException {

    private final String currentStatus;

    public OrderNotCancellableException(String currentStatus) {
        super("Order cannot be cancelled in status: " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
