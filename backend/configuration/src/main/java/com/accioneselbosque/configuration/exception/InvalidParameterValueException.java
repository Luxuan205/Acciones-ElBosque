package com.accioneselbosque.configuration.exception;

public class InvalidParameterValueException extends RuntimeException {

    private final String parameterKey;

    public InvalidParameterValueException(String parameterKey, String reason) {
        super("Invalid value for parameter '" + parameterKey + "': " + reason);
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return parameterKey;
    }
}
