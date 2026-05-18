package com.accioneselbosque.orders.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente para cubrir el total de la compra.");
    }
}
