package com.accioneselbosque.market_data_service.exception;

public class SymbolNotInWatchlistException extends RuntimeException {
    public SymbolNotInWatchlistException(String symbol) {
        super(symbol + " is not in your watchlist");
    }
}
