package com.accioneselbosque.market_data_service.repository;

import com.accioneselbosque.market_data_service.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    Optional<Watchlist> findByInvestorId(Long investorId);
}
