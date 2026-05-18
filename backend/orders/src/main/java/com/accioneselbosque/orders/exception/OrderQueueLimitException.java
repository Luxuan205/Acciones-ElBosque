package com.accioneselbosque.orders.exception;

public class OrderQueueLimitException extends RuntimeException {
    public OrderQueueLimitException() {
        super("Has alcanzado el límite de 10 órdenes en cola.");
    }
}
