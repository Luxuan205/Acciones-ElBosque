package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.portfolio.dto.PortfolioHistoryPoint;
import com.accioneselbosque.portfolio.dto.PortfolioHistoryResponse;
import com.accioneselbosque.portfolio.model.PortfolioSnapshot;
import com.accioneselbosque.portfolio.repository.AccountBalanceRepository;
import com.accioneselbosque.portfolio.repository.PortfolioSnapshotRepository;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final PositionRepository positionRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final StockSnapshotService stockSnapshotService;

    @Transactional
    public void takeSnapshot() {
        List<Long> investorIds = positionRepository.findDistinctInvestorIds();
        LocalDate today = LocalDate.now();
        log.info("PortfolioSnapshotService: taking snapshot for {} investors", investorIds.size());

        for (Long investorId : investorIds) {
            BigDecimal positionsValue = positionRepository.findByInvestorId(investorId).stream()
                    .map(p -> {
                        BigDecimal price = stockSnapshotService.findBySymbol(p.getSymbol())
                                .map(s -> s.getCurrentPrice())
                                .orElse(p.getAvgPurchasePrice());
                        return price.multiply(BigDecimal.valueOf(p.getCurrentQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cash = accountBalanceRepository.findByInvestorId(investorId)
                    .map(b -> b.getTotalBalance())
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalValue = cash.add(positionsValue);

            PortfolioSnapshot snapshot = snapshotRepository
                    .findByInvestorIdAndSnapshotDate(investorId, today)
                    .orElseGet(() -> PortfolioSnapshot.builder()
                            .investorId(investorId)
                            .snapshotDate(today)
                            .build());
            snapshot.setTotalValue(totalValue);
            snapshotRepository.save(snapshot);
        }
        log.info("PortfolioSnapshotService: snapshot complete");
    }

    @Transactional(readOnly = true)
    public PortfolioHistoryResponse getHistory(Long investorId, String period) {
        LocalDate from = switch (period) {
            case "7D" -> LocalDate.now().minusDays(7);
            case "3M" -> LocalDate.now().minusDays(90);
            case "1A" -> LocalDate.now().minusDays(365);
            default   -> LocalDate.now().minusDays(30);
        };

        List<PortfolioHistoryPoint> points = snapshotRepository
                .findByInvestorIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        investorId, from, LocalDate.now())
                .stream()
                .map(s -> new PortfolioHistoryPoint(s.getSnapshotDate(), s.getTotalValue()))
                .toList();

        return new PortfolioHistoryResponse(points);
    }
}
