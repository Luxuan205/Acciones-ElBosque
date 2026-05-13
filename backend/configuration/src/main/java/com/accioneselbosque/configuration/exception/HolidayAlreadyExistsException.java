package com.accioneselbosque.configuration.exception;

import java.time.LocalDate;

public class HolidayAlreadyExistsException extends RuntimeException {
    public HolidayAlreadyExistsException(LocalDate date) {
        super("A holiday is already configured for " + date);
    }
}
