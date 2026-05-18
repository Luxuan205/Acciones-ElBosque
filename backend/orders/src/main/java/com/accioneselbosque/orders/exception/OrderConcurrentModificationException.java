package com.accioneselbosque.orders.exception;

public class OrderConcurrentModificationException extends RuntimeException {

    private final String currentStatus;

    public OrderConcurrentModificationException(String currentStatus) {
        super("Order was modified concurrently. Current status: " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
