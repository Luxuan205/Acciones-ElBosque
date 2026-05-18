package com.accioneselbosque.app.exception;

public class InvalidDashboardPeriodException extends RuntimeException {
    public InvalidDashboardPeriodException(String period) {
        super("Invalid dashboard period: " + period + ". Valid values: TODAY, WEEK, MONTH");
    }
}
