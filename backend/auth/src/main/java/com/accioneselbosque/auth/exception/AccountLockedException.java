package com.accioneselbosque.auth.exception;

public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
        super("Account is temporarily locked");
    }
}
