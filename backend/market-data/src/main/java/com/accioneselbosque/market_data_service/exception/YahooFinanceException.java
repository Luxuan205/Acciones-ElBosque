package com.accioneselbosque.market_data_service.exception;

public class YahooFinanceException extends RuntimeException {
    public YahooFinanceException(String message) {
        super(message);
    }

    public YahooFinanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
