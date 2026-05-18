package com.accioneselbosque.market_data_service.repository;

import com.accioneselbosque.market_data_service.model.IntradayPricePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IntradayPricePointRepository extends JpaRepository<IntradayPricePoint, UUID> {

    List<IntradayPricePoint> findBySymbolAndTimestampBetween(String symbol, LocalDateTime from, LocalDateTime to);

    void deleteByTimestampBefore(LocalDateTime cutoff);
}
