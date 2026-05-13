package com.accioneselbosque.auth.exception;

public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
