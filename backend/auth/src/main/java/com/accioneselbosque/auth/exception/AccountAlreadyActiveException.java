package com.accioneselbosque.auth.exception;

public class AccountAlreadyActiveException extends RuntimeException {

    public AccountAlreadyActiveException() {
        super("Account is already active");
    }
}
