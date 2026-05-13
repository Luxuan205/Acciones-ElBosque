package com.accioneselbosque.market_data_service.repository;

import com.accioneselbosque.market_data_service.model.WatchlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, UUID> {

    long countByWatchlistId(UUID watchlistId);

    List<WatchlistEntry> findByWatchlistIdOrderByAddedAtDesc(UUID watchlistId);

    Optional<WatchlistEntry> findByWatchlistIdAndSymbol(UUID watchlistId, String symbol);

    void deleteByWatchlistIdAndSymbol(UUID watchlistId, String symbol);
}
