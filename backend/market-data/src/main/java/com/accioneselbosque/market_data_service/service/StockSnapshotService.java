package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.dto.IntradayDataDto;
import com.accioneselbosque.market_data_service.dto.IntradayPointDto;
import com.accioneselbosque.market_data_service.dto.StockDetailDto;
import com.accioneselbosque.market_data_service.dto.StockSummaryDto;
import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.repository.IntradayPricePointRepository;
import com.accioneselbosque.market_data_service.repository.StockSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockSnapshotService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "name_asc", "name_desc", "dayChangePct_asc", "dayChangePct_desc"
    );

    private final StockSnapshotRepository snapshotRepository;
    private final IntradayPricePointRepository intradayRepository;
    private final MarketStatusService marketStatusService;

    @Transactional(readOnly = true)
    public Optional<StockSnapshot> findBySymbol(String symbol) {
        return snapshotRepository.findBySymbol(symbol);
    }

    @Transactional(readOnly = true)
    public List<StockSummaryDto> listStocks(String search, String sort) {
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new IllegalArgumentException(
                    "Invalid sort value '" + sort + "'. Allowed: " + ALLOWED_SORTS);
        }

        Sort springSort = buildSort(sort);
        boolean stale = !marketStatusService.isMarketOpen();

        List<StockSnapshot> snapshots;
        if (search != null && !search.isBlank()) {
            snapshots = snapshotRepository
                    .findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(search, search, springSort);
        } else {
            snapshots = snapshotRepository.findAll(springSort);
        }

        return snapshots.stream()
                .map(s -> new StockSummaryDto(
                        s.getSymbol(), s.getName(),
                        s.getCurrentPrice(), s.getPreviousClose(),
                        s.getDayChange(), s.getDayChangePct(),
                        s.getVolume(), s.getUpdatedAt(), stale))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockDetailDto getDetail(String symbol) {
        StockSnapshot s = snapshotRepository.findBySymbol(symbol)
                .orElseThrow(() -> new SymbolNotFoundException(symbol));
        boolean marketOpen = marketStatusService.isMarketOpen();
        boolean stale = !marketOpen;
        return new StockDetailDto(
                s.getSymbol(), s.getName(),
                s.getCurrentPrice(), s.getPreviousClose(),
                s.getDayChange(), s.getDayChangePct(),
                s.getVolume(), s.getUpdatedAt(), stale, marketOpen);
    }

    @Transactional(readOnly = true)
    public IntradayDataDto getIntraday(String symbol) {
        snapshotRepository.findBySymbol(symbol)
                .orElseThrow(() -> new SymbolNotFoundException(symbol));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<IntradayPointDto> points = intradayRepository
                .findBySymbolAndTimestampBetween(symbol, startOfToday, now)
                .stream()
                .map(p -> new IntradayPointDto(p.getTimestamp(), p.getPrice(), p.getVolume()))
                .collect(Collectors.toList());

        return new IntradayDataDto(symbol, LocalDate.now(), "5min", points);
    }

    private Sort buildSort(String sort) {
        String[] parts = sort.split("_(?=[^_]+$)"); // split on last underscore
        String field = parts[0];
        Sort.Direction direction = "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
