package com.accioneselbosque.orders.exception;

public class InsufficientTitlesException extends RuntimeException {

    public InsufficientTitlesException() {
        super("Insufficient titles to cover the sell order.");
    }
}
