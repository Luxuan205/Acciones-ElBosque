package com.accioneselbosque.notifications.exception;

public class MarketAlertNotFoundException extends RuntimeException {
    public MarketAlertNotFoundException(Long id) {
        super("Market alert subscription not found: " + id);
    }
}
