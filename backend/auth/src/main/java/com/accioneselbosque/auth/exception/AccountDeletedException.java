package com.accioneselbosque.auth.exception;

public class AccountDeletedException extends RuntimeException {
    public AccountDeletedException() {
        super("Cuenta eliminada");
    }
}
