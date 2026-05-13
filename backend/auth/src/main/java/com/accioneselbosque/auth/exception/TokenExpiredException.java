package com.accioneselbosque.auth.exception;

public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException() {
        super("Verification token has expired");
    }
}
