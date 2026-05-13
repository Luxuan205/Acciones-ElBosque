package com.accioneselbosque.market_data_service.exception;

public class WatchlistLimitReachedException extends RuntimeException {
    public WatchlistLimitReachedException() {
        super("Watchlist limit reached (max 50 symbols). Remove a symbol before adding a new one.");
    }
}
