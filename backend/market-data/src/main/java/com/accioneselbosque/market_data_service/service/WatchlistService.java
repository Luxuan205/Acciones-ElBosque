package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.market_data_service.dto.*;
import com.accioneselbosque.market_data_service.exception.SymbolAlreadyInWatchlistException;
import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import com.accioneselbosque.market_data_service.exception.SymbolNotInWatchlistException;
import com.accioneselbosque.market_data_service.exception.WatchlistLimitReachedException;
import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.model.Watchlist;
import com.accioneselbosque.market_data_service.model.WatchlistEntry;
import com.accioneselbosque.market_data_service.repository.WatchlistEntryRepository;
import com.accioneselbosque.market_data_service.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private static final int MAX_ENTRIES = 50;

    private final WatchlistRepository watchlistRepository;
    private final WatchlistEntryRepository watchlistEntryRepository;
    private final PremiumSubscriptionGate premiumGate;
    private final StockSnapshotService snapshotService;

    @Transactional
    public Watchlist getOrCreateWatchlist(Long investorId) {
        return watchlistRepository.findByInvestorId(investorId).orElseGet(() -> {
            Watchlist w = new Watchlist();
            w.setInvestorId(investorId);
            return watchlistRepository.save(w);
        });
    }

    @Transactional
    public WatchlistResponse getWatchlist(Long investorId) {
        premiumGate.assertIsPremiumActive(investorId);

        Watchlist watchlist = watchlistRepository.findByInvestorId(investorId).orElseGet(() -> {
            Watchlist w = new Watchlist();
            w.setInvestorId(investorId);
            return watchlistRepository.save(w);
        });

        List<WatchlistEntry> entries = watchlistEntryRepository
                .findByWatchlistIdOrderByAddedAtDesc(watchlist.getId());

        List<WatchlistEntryDto> dtos = entries.stream()
                .map(entry -> {
                    StockSnapshot snap = snapshotService.findBySymbol(entry.getSymbol()).orElse(null);
                    return new WatchlistEntryDto(
                            entry.getSymbol(),
                            snap != null ? snap.getName() : null,
                            snap != null ? snap.getCurrentPrice() : null,
                            entry.getPriceAtAdded(),
                            snap != null ? snap.getDayChange() : null,
                            snap != null ? snap.getDayChangePct() : null,
                            snap != null ? snap.getUpdatedAt() : null,
                            entry.getAddedAt()
                    );
                })
                .toList();

        return new WatchlistResponse(
                watchlist.getId(),
                investorId,
                dtos.size(),
                MAX_ENTRIES,
                dtos
        );
    }

    @Transactional
    public AddEntryResponse addEntry(Long investorId, String symbol) {
        premiumGate.assertIsPremiumActive(investorId);

        Watchlist watchlist = getOrCreateWatchlist(investorId);

        StockSnapshot snapshot = snapshotService.findBySymbol(symbol)
                .orElseThrow(() -> new SymbolNotFoundException(symbol));

        if (watchlistEntryRepository.countByWatchlistId(watchlist.getId()) >= MAX_ENTRIES) {
            throw new WatchlistLimitReachedException();
        }

        if (watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlist.getId(), symbol).isPresent()) {
            throw new SymbolAlreadyInWatchlistException(symbol);
        }

        WatchlistEntry entry = new WatchlistEntry();
        entry.setWatchlist(watchlist);
        entry.setSymbol(symbol);
        entry.setPriceAtAdded(snapshot.getCurrentPrice());
        watchlistEntryRepository.save(entry);

        long count = watchlistEntryRepository.countByWatchlistId(watchlist.getId());

        return new AddEntryResponse(
                symbol,
                snapshot.getName(),
                snapshot.getCurrentPrice(),
                entry.getAddedAt(),
                (int) count,
                MAX_ENTRIES
        );
    }

    @Transactional
    public RemoveEntryResponse removeEntry(Long investorId, String symbol) {
        premiumGate.assertIsPremiumActive(investorId);

        Watchlist watchlist = getOrCreateWatchlist(investorId);

        watchlistEntryRepository.findByWatchlistIdAndSymbol(watchlist.getId(), symbol)
                .orElseThrow(() -> new SymbolNotInWatchlistException(symbol));

        watchlistEntryRepository.deleteByWatchlistIdAndSymbol(watchlist.getId(), symbol);

        long count = watchlistEntryRepository.countByWatchlistId(watchlist.getId());

        return new RemoveEntryResponse(symbol, LocalDateTime.now(), (int) count, MAX_ENTRIES);
    }
}
