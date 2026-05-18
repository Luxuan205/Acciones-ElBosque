package com.accioneselbosque.auth.exception;

public class MfaSessionExpiredException extends RuntimeException {

    public MfaSessionExpiredException() {
        super("MFA session has expired");
    }
}
