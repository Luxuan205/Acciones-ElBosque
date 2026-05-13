package com.accioneselbosque.auth.exception;

public class TokenNotFoundException extends RuntimeException {

    public TokenNotFoundException() {
        super("Verification token not found");
    }
}
