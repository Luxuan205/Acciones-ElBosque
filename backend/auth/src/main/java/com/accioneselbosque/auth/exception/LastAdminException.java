package com.accioneselbosque.auth.exception;

public class LastAdminException extends RuntimeException {
    public LastAdminException() {
        super("Cannot modify the last active administrator.");
    }
}
