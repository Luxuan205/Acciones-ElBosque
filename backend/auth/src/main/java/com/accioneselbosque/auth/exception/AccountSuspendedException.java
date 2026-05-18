package com.accioneselbosque.auth.exception;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("Account has been suspended");
    }
}
