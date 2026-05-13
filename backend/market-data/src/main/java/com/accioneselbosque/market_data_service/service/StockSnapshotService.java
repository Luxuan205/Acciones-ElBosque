package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.repository.StockSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockSnapshotService {

    private final StockSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public Optional<StockSnapshot> findBySymbol(String symbol) {
        return snapshotRepository.findBySymbol(symbol);
    }
}
