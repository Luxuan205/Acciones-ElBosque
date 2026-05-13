package com.accioneselbosque.market_data_service.controller;

import com.accioneselbosque.market_data_service.dto.*;
import com.accioneselbosque.market_data_service.exception.PremiumRequiredException;
import com.accioneselbosque.market_data_service.exception.SymbolAlreadyInWatchlistException;
import com.accioneselbosque.market_data_service.exception.SymbolNotInWatchlistException;
import com.accioneselbosque.market_data_service.exception.WatchlistLimitReachedException;
import com.accioneselbosque.market_data_service.service.WatchlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WatchlistControllerTest {

    @Mock
    private WatchlistService watchlistService;

    private MockMvc mockMvc;

    // Jackson 2.x ObjectMapper for serializing request bodies in tests
    private final ObjectMapper requestMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Principals reused across tests — username is the investor ID as string
    private static final UsernamePasswordAuthenticationToken AUTH_1 =
            UsernamePasswordAuthenticationToken.authenticated("1", null, List.of());
    private static final UsernamePasswordAuthenticationToken AUTH_2 =
            UsernamePasswordAuthenticationToken.authenticated("2", null, List.of());
    private static final UsernamePasswordAuthenticationToken AUTH_3 =
            UsernamePasswordAuthenticationToken.authenticated("3", null, List.of());

    @BeforeEach
    void setUp() {
        // JacksonJsonHttpMessageConverter (Jackson 3.x) with auto-discovered modules
        // handles response serialization.
        // Authentication is set via .principal() so Spring MVC resolves it from
        // request.getUserPrincipal() without needing the Spring Security filter chain.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WatchlistController(watchlistService))
                .setControllerAdvice(new com.accioneselbosque.market_data_service.exception.GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    // ── US1: GET /watchlist ────────────────────────────────────────────────────

    @Test
    void getWatchlist_premium_returns200WithEntries() throws Exception {
        WatchlistEntryDto entry = new WatchlistEntryDto(
                "PFBCOLOM", "Bancolombia Preferencial",
                new BigDecimal("39500.00"), new BigDecimal("38200.00"),
                new BigDecimal("350.00"), new BigDecimal("0.8946"),
                LocalDateTime.now(), LocalDateTime.now().minusDays(10));

        WatchlistResponse response = new WatchlistResponse(
                UUID.randomUUID(), 1L, 1, 50, List.of(entry));

        when(watchlistService.getWatchlist(1L)).thenReturn(response);

        mockMvc.perform(get("/watchlist").principal(AUTH_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.maxEntries").value(50))
                .andExpect(jsonPath("$.entries[0].symbol").value("PFBCOLOM"));
    }

    @Test
    void getWatchlist_standard_returns403() throws Exception {
        when(watchlistService.getWatchlist(2L)).thenThrow(new PremiumRequiredException());

        mockMvc.perform(get("/watchlist").principal(AUTH_2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PREMIUM_REQUIRED"));
    }

    @Test
    void getWatchlist_firstAccess_returns200WithEmptyList() throws Exception {
        WatchlistResponse empty = new WatchlistResponse(
                UUID.randomUUID(), 3L, 0, 50, List.of());

        when(watchlistService.getWatchlist(3L)).thenReturn(empty);

        mockMvc.perform(get("/watchlist").principal(AUTH_3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(0))
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    // ── US2: POST /watchlist/entries ───────────────────────────────────────────

    @Test
    void addEntry_valid_returns201() throws Exception {
        AddEntryResponse response = new AddEntryResponse(
                "ECOPETROL", "Ecopetrol S.A.",
                new BigDecimal("1972.00"), LocalDateTime.now(), 1, 50);

        when(watchlistService.addEntry(eq(1L), eq("ECOPETROL"))).thenReturn(response);

        mockMvc.perform(post("/watchlist/entries")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(new WatchlistEntryRequest("ECOPETROL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("ECOPETROL"))
                .andExpect(jsonPath("$.entryCount").value(1));
    }

    @Test
    void addEntry_symbolNotInCatalog_returns400() throws Exception {
        when(watchlistService.addEntry(eq(1L), eq("FAKESYM")))
                .thenThrow(new com.accioneselbosque.market_data_service.exception.SymbolNotFoundException("FAKESYM"));

        mockMvc.perform(post("/watchlist/entries")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(new WatchlistEntryRequest("FAKESYM"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SYMBOL_NOT_FOUND"));
    }

    @Test
    void addEntry_duplicate_returns409() throws Exception {
        when(watchlistService.addEntry(eq(1L), eq("PFBCOLOM")))
                .thenThrow(new SymbolAlreadyInWatchlistException("PFBCOLOM"));

        mockMvc.perform(post("/watchlist/entries")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(new WatchlistEntryRequest("PFBCOLOM"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SYMBOL_ALREADY_IN_WATCHLIST"));
    }

    @Test
    void addEntry_limitReached_returns422() throws Exception {
        when(watchlistService.addEntry(eq(1L), anyString()))
                .thenThrow(new WatchlistLimitReachedException());

        mockMvc.perform(post("/watchlist/entries")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(new WatchlistEntryRequest("ECOPETROL"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WATCHLIST_LIMIT_REACHED"));
    }

    @Test
    void addEntry_standard_returns403() throws Exception {
        when(watchlistService.addEntry(eq(2L), anyString()))
                .thenThrow(new PremiumRequiredException());

        mockMvc.perform(post("/watchlist/entries")
                        .principal(AUTH_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(new WatchlistEntryRequest("ECOPETROL"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PREMIUM_REQUIRED"));
    }

    // ── US2: DELETE /watchlist/entries/{symbol} ────────────────────────────────

    @Test
    void removeEntry_exists_returns200() throws Exception {
        RemoveEntryResponse response = new RemoveEntryResponse(
                "PFBCOLOM", LocalDateTime.now(), 0, 50);

        when(watchlistService.removeEntry(eq(1L), eq("PFBCOLOM"))).thenReturn(response);

        mockMvc.perform(delete("/watchlist/entries/PFBCOLOM").principal(AUTH_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("PFBCOLOM"))
                .andExpect(jsonPath("$.entryCount").value(0));
    }

    @Test
    void removeEntry_notInWatchlist_returns404() throws Exception {
        when(watchlistService.removeEntry(eq(1L), eq("PFBCOLOM")))
                .thenThrow(new SymbolNotInWatchlistException("PFBCOLOM"));

        mockMvc.perform(delete("/watchlist/entries/PFBCOLOM").principal(AUTH_1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SYMBOL_NOT_IN_WATCHLIST"));
    }
}
