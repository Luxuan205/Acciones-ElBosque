package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.market_data_service.dto.AddEntryResponse;
import com.accioneselbosque.market_data_service.dto.RemoveEntryResponse;
import com.accioneselbosque.market_data_service.dto.WatchlistResponse;
import com.accioneselbosque.market_data_service.exception.SymbolAlreadyInWatchlistException;
import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import com.accioneselbosque.market_data_service.exception.SymbolNotInWatchlistException;
import com.accioneselbosque.market_data_service.exception.WatchlistLimitReachedException;
import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.model.Watchlist;
import com.accioneselbosque.market_data_service.model.WatchlistEntry;
import com.accioneselbosque.market_data_service.repository.WatchlistEntryRepository;
import com.accioneselbosque.market_data_service.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock private WatchlistRepository watchlistRepository;
    @Mock private WatchlistEntryRepository watchlistEntryRepository;
    @Mock private PremiumSubscriptionGate premiumGate;
    @Mock private StockSnapshotService snapshotService;

    @InjectMocks
    private WatchlistService watchlistService;

    private static final Long INVESTOR_ID = 1L;
    private Watchlist watchlist;
    private UUID watchlistId;

    @BeforeEach
    void setUp() {
        watchlistId = UUID.randomUUID();
        watchlist = new Watchlist();
        watchlist.setId(watchlistId);
        watchlist.setInvestorId(INVESTOR_ID);
        watchlist.setCreatedAt(LocalDateTime.now());
    }

    // ── getOrCreateWatchlist ──────────────────────────────────────────────────

    @Test
    void getOrCreateWatchlist_existing_returnsExisting() {
        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));

        Watchlist result = watchlistService.getOrCreateWatchlist(INVESTOR_ID);

        assertThat(result).isSameAs(watchlist);
        verify(watchlistRepository, never()).save(any());
    }

    @Test
    void getOrCreateWatchlist_notExists_createsNew() {
        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.empty());
        when(watchlistRepository.save(any(Watchlist.class))).thenReturn(watchlist);

        Watchlist result = watchlistService.getOrCreateWatchlist(INVESTOR_ID);

        assertThat(result.getInvestorId()).isEqualTo(INVESTOR_ID);
        verify(watchlistRepository).save(any(Watchlist.class));
    }

    // ── getWatchlist ──────────────────────────────────────────────────────────

    @Test
    void getWatchlist_withEntries_enrichesFromSnapshot() {
        WatchlistEntry entry = new WatchlistEntry();
        entry.setSymbol("PFBCOLOM");
        entry.setPriceAtAdded(new BigDecimal("38200.00"));
        entry.setAddedAt(LocalDateTime.now().minusDays(1));
        entry.setWatchlist(watchlist);

        StockSnapshot snap = new StockSnapshot();
        snap.setSymbol("PFBCOLOM");
        snap.setName("Bancolombia Preferencial");
        snap.setCurrentPrice(new BigDecimal("39500.00"));
        snap.setDayChange(new BigDecimal("350.00"));
        snap.setDayChangePct(new BigDecimal("0.8946"));
        snap.setUpdatedAt(LocalDateTime.now());

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(watchlistEntryRepository.findByWatchlistIdOrderByAddedAtDesc(watchlistId))
                .thenReturn(List.of(entry));
        when(snapshotService.findBySymbol("PFBCOLOM")).thenReturn(Optional.of(snap));

        WatchlistResponse response = watchlistService.getWatchlist(INVESTOR_ID);

        assertThat(response.entryCount()).isEqualTo(1);
        assertThat(response.entries().get(0).currentPrice()).isEqualByComparingTo("39500.00");
        assertThat(response.entries().get(0).name()).isEqualTo("Bancolombia Preferencial");
    }

    @Test
    void getWatchlist_snapshotNull_returnsNullPriceFields() {
        WatchlistEntry entry = new WatchlistEntry();
        entry.setSymbol("UNKNOWN");
        entry.setPriceAtAdded(new BigDecimal("100.00"));
        entry.setAddedAt(LocalDateTime.now());
        entry.setWatchlist(watchlist);

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(watchlistEntryRepository.findByWatchlistIdOrderByAddedAtDesc(watchlistId))
                .thenReturn(List.of(entry));
        when(snapshotService.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        WatchlistResponse response = watchlistService.getWatchlist(INVESTOR_ID);

        assertThat(response.entries().get(0).currentPrice()).isNull();
        assertThat(response.entries().get(0).dayChange()).isNull();
    }

    // ── addEntry ──────────────────────────────────────────────────────────────

    @Test
    void addEntry_valid_persistsAndReturns() {
        StockSnapshot snap = new StockSnapshot();
        snap.setSymbol("ECOPETROL");
        snap.setName("Ecopetrol S.A.");
        snap.setCurrentPrice(new BigDecimal("1972.00"));

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(snapshotService.findBySymbol("ECOPETROL")).thenReturn(Optional.of(snap));
        when(watchlistEntryRepository.countByWatchlistId(watchlistId)).thenReturn(0L, 1L);
        when(watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlistId, "ECOPETROL"))
                .thenReturn(Optional.empty());
        when(watchlistEntryRepository.save(any(WatchlistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        AddEntryResponse response = watchlistService.addEntry(INVESTOR_ID, "ECOPETROL");

        assertThat(response.symbol()).isEqualTo("ECOPETROL");
        assertThat(response.priceAtAdded()).isEqualByComparingTo("1972.00");
        verify(watchlistEntryRepository).save(any(WatchlistEntry.class));
    }

    @Test
    void addEntry_symbolNotInCatalog_throwsSymbolNotFoundException() {
        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(snapshotService.findBySymbol("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.addEntry(INVESTOR_ID, "XYZ"))
                .isInstanceOf(SymbolNotFoundException.class);
    }

    @Test
    void addEntry_limitReached_throwsWatchlistLimitReachedException() {
        StockSnapshot snap = new StockSnapshot();
        snap.setCurrentPrice(BigDecimal.TEN);

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(snapshotService.findBySymbol("ECOPETROL")).thenReturn(Optional.of(snap));
        when(watchlistEntryRepository.countByWatchlistId(watchlistId)).thenReturn(50L);

        assertThatThrownBy(() -> watchlistService.addEntry(INVESTOR_ID, "ECOPETROL"))
                .isInstanceOf(WatchlistLimitReachedException.class);
    }

    @Test
    void addEntry_duplicate_throwsSymbolAlreadyInWatchlistException() {
        StockSnapshot snap = new StockSnapshot();
        snap.setCurrentPrice(BigDecimal.TEN);

        WatchlistEntry existing = new WatchlistEntry();
        existing.setSymbol("PFBCOLOM");

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(snapshotService.findBySymbol("PFBCOLOM")).thenReturn(Optional.of(snap));
        when(watchlistEntryRepository.countByWatchlistId(watchlistId)).thenReturn(1L);
        when(watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlistId, "PFBCOLOM"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> watchlistService.addEntry(INVESTOR_ID, "PFBCOLOM"))
                .isInstanceOf(SymbolAlreadyInWatchlistException.class);
    }

    // ── removeEntry ───────────────────────────────────────────────────────────

    @Test
    void removeEntry_exists_deletesAndReturnsUpdatedCount() {
        WatchlistEntry entry = new WatchlistEntry();
        entry.setSymbol("PFBCOLOM");
        entry.setWatchlist(watchlist);

        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlistId, "PFBCOLOM"))
                .thenReturn(Optional.of(entry));
        when(watchlistEntryRepository.countByWatchlistId(watchlistId)).thenReturn(0L);

        RemoveEntryResponse response = watchlistService.removeEntry(INVESTOR_ID, "PFBCOLOM");

        assertThat(response.symbol()).isEqualTo("PFBCOLOM");
        assertThat(response.entryCount()).isEqualTo(0);
        verify(watchlistEntryRepository).deleteByWatchlistIdAndSymbol(watchlistId, "PFBCOLOM");
    }

    @Test
    void removeEntry_notInWatchlist_throwsSymbolNotInWatchlistException() {
        when(watchlistRepository.findByInvestorId(INVESTOR_ID)).thenReturn(Optional.of(watchlist));
        when(watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlistId, "PFBCOLOM"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.removeEntry(INVESTOR_ID, "PFBCOLOM"))
                .isInstanceOf(SymbolNotInWatchlistException.class);
    }
}
