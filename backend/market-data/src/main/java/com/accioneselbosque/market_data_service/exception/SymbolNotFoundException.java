package com.accioneselbosque.market_data_service.exception;

public class SymbolNotFoundException extends RuntimeException {
    public SymbolNotFoundException(String symbol) {
        super("No stock found with symbol: " + symbol);
    }
}
