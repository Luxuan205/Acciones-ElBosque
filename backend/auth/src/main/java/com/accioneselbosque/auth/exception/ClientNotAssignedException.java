package com.accioneselbosque.auth.exception;

public class ClientNotAssignedException extends RuntimeException {
    public ClientNotAssignedException() {
        super("El cliente no está asignado a este comisionista.");
    }
}
