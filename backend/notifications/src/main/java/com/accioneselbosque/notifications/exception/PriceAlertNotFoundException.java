package com.accioneselbosque.notifications.exception;

public class PriceAlertNotFoundException extends RuntimeException {
    public PriceAlertNotFoundException(Long id) {
        super("Price alert not found: " + id);
    }
}
