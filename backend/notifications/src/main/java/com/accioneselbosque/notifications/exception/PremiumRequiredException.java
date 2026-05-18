package com.accioneselbosque.notifications.exception;

public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException() {
        super("Se requiere suscripción Premium para esta función.");
    }
}
