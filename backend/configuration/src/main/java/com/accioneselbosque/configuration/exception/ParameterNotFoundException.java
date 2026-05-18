package com.accioneselbosque.configuration.exception;

public class ParameterNotFoundException extends RuntimeException {
    public ParameterNotFoundException(String key) {
        super("Parameter not found: " + key);
    }
}
