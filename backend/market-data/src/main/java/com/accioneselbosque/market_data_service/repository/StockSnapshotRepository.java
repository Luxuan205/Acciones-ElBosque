package com.accioneselbosque.market_data_service.repository;

import com.accioneselbosque.market_data_service.model.StockSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, UUID> {

    Optional<StockSnapshot> findBySymbol(String symbol);
}
