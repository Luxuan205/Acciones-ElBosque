package com.accioneselbosque.auth.exception;

public class TokenAlreadyUsedException extends RuntimeException {

    public TokenAlreadyUsedException() {
        super("Verification token has already been used");
    }
}
