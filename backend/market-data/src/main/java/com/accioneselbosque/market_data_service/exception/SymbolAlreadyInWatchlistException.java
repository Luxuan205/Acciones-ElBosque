package com.accioneselbosque.market_data_service.exception;

public class SymbolAlreadyInWatchlistException extends RuntimeException {
    public SymbolAlreadyInWatchlistException(String symbol) {
        super(symbol + " is already in your watchlist");
    }
}
