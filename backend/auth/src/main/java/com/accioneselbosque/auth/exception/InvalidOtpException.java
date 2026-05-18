package com.accioneselbosque.auth.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("Invalid or expired OTP code");
    }
}
