package com.accioneselbosque.auth.exception;

public class AdminConfirmationRequiredException extends RuntimeException {
    public AdminConfirmationRequiredException() {
        super("Assigning the ADMIN role requires explicit confirmation (confirmed=true).");
    }
}
