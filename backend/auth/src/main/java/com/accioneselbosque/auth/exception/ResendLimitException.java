package com.accioneselbosque.auth.exception;

public class ResendLimitException extends RuntimeException {

    public ResendLimitException() {
        super("Resend limit exceeded");
    }
}
