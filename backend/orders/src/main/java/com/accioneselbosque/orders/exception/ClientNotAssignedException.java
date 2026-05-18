package com.accioneselbosque.orders.exception;

public class ClientNotAssignedException extends RuntimeException {
    public ClientNotAssignedException() {
        super("El cliente no está asignado a este comisionista.");
    }
}
