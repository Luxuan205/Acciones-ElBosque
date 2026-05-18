package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.portfolio.dto.PortfolioPositionsResponse;
import com.accioneselbosque.portfolio.dto.PortfolioReportDto;
import com.accioneselbosque.portfolio.dto.PositionDto;
import com.accioneselbosque.portfolio.dto.TransactionDto;
import com.accioneselbosque.portfolio.model.Position;
import com.accioneselbosque.portfolio.model.ReportPeriod;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import com.accioneselbosque.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final StockSnapshotService stockSnapshotService;
    private final PeriodResolver periodResolver;

    @Transactional(readOnly = true)
    public PortfolioPositionsResponse getPositions(Long investorId) {
        List<Position> positions = positionRepository.findByInvestorId(investorId);

        if (positions.isEmpty()) {
            return new PortfolioPositionsResponse(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedGain = BigDecimal.ZERO;

        List<PositionDto> positionDtos = new java.util.ArrayList<>();

        for (Position pos : positions) {
            var snapshot = stockSnapshotService.findBySymbol(pos.getSymbol());
            BigDecimal currentPrice = snapshot.map(s -> s.getCurrentPrice()).orElse(pos.getAvgPurchasePrice());
            BigDecimal dayChange = snapshot.map(s -> s.getDayChange()).orElse(BigDecimal.ZERO);
            String stockName = snapshot.map(s -> s.getName() != null ? s.getName() : pos.getSymbol()).orElse(pos.getSymbol());

            int qty = pos.getCurrentQuantity();
            BigDecimal avgPrice = pos.getAvgPurchasePrice();

            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal unrealizedGain = currentPrice.subtract(avgPrice).multiply(BigDecimal.valueOf(qty));

            BigDecimal cost = avgPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal unrealizedGainPct;
            if (cost.compareTo(BigDecimal.ZERO) == 0) {
                unrealizedGainPct = BigDecimal.ZERO;
            } else {
                unrealizedGainPct = unrealizedGain.divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            positionDtos.add(new PositionDto(
                    pos.getSymbol(),
                    stockName,
                    qty,
                    avgPrice,
                    currentPrice,
                    marketValue,
                    unrealizedGain,
                    unrealizedGainPct,
                    dayChange,
                    "COP"
            ));

            totalCost = totalCost.add(cost);
            totalValue = totalValue.add(marketValue);
            totalUnrealizedGain = totalUnrealizedGain.add(unrealizedGain);
        }

        BigDecimal totalUnrealizedGainPct;
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            totalUnrealizedGainPct = BigDecimal.ZERO;
        } else {
            totalUnrealizedGainPct = totalUnrealizedGain.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new PortfolioPositionsResponse(positionDtos, totalValue, totalUnrealizedGain, totalUnrealizedGainPct);
    }

    @Transactional(readOnly = true)
    public PortfolioReportDto getReport(Long investorId, ReportPeriod period, LocalDate from, LocalDate to) {
        LocalDate[] range = periodResolver.resolve(period, from, to);
        LocalDate resolvedFrom = range[0];
        LocalDate resolvedTo = range[1];

        PortfolioPositionsResponse positionsResponse = getPositions(investorId);

        List<Transaction> transactions = transactionRepository.findByInvestorIdAndExecutedAtBetween(
                investorId,
                resolvedFrom.atStartOfDay(),
                resolvedTo.atTime(23, 59, 59)
        );

        BigDecimal totalRealizedGain = transactions.stream()
                .filter(t -> t.getRealizedGain() != null)
                .map(Transaction::getRealizedGain)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionDto> transactionDtos = transactions.stream()
                .map(t -> new TransactionDto(
                        t.getTransactionType().name(),
                        t.getSymbol(),
                        t.getQuantity(),
                        t.getExecutionPrice(),
                        t.getCommission(),
                        t.getGrossAmount(),
                        t.getNetAmount(),
                        t.getRealizedGain(),
                        t.getExecutedAt()
                ))
                .toList();

        return new PortfolioReportDto(
                period.name(),
                resolvedFrom,
                resolvedTo,
                totalRealizedGain,
                positionsResponse.positions(),
                transactionDtos
        );
    }
}
