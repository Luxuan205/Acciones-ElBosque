package com.accioneselbosque.configuration.exception;

public class HolidayNotFoundException extends RuntimeException {
    public HolidayNotFoundException() {
        super("Holiday not found");
    }
}
