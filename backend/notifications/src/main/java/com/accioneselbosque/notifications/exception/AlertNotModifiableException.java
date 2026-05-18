package com.accioneselbosque.notifications.exception;

public class AlertNotModifiableException extends RuntimeException {
    public AlertNotModifiableException(Long id) {
        super("Alert cannot be modified in its current state: " + id);
    }
}
