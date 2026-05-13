package com.accioneselbosque.auth.exception;

public class ResendRateLimitException extends RuntimeException {

    public ResendRateLimitException() {
        super("Please wait before requesting another verification email");
    }
}
