package com.accioneselbosque.market_data_service.exception;

public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException() {
        super("Active PREMIUM subscription required to access watchlist");
    }
}
