package com.accioneselbosque.auth.exception;

public class CannotDeleteAccountException extends RuntimeException {
    public CannotDeleteAccountException(String reason) {
        super(reason);
    }
}
