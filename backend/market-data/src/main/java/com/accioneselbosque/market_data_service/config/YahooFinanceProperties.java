package com.accioneselbosque.market_data_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.market.yahoo-finance")
@Getter
@Setter
public class YahooFinanceProperties {
    private String baseUrl = "https://query1.finance.yahoo.com";
    private long refreshIntervalMs = 1_800_000L;
    private Map<String, String> symbolMapping = new HashMap<>();
}
