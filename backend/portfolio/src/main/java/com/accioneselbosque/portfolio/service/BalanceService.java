package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.portfolio.dto.BalanceSummaryResponse;
import com.accioneselbosque.portfolio.model.AccountBalance;
import com.accioneselbosque.portfolio.model.Position;
import com.accioneselbosque.portfolio.repository.AccountBalanceRepository;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final AccountBalanceRepository accountBalanceRepository;
    private final PositionRepository positionRepository;
    private final StockSnapshotService stockSnapshotService;
    private final InvestorRepository investorRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public BalanceSummaryResponse getBalance(Long investorId) {
        AccountBalance balance = accountBalanceRepository.findByInvestorId(investorId)
                .orElseGet(() -> {
                    BigDecimal initialBalance = investorRepository.findById(investorId)
                            .map(inv -> inv.getAvailableBalance())
                            .orElse(BigDecimal.valueOf(5_000_000));
                    AccountBalance newBalance = AccountBalance.builder()
                            .investorId(investorId)
                            .totalBalance(initialBalance)
                            .currency("COP")
                            .build();
                    return accountBalanceRepository.save(newBalance);
                });

        BigDecimal reservedForOrders = (BigDecimal) em.createNativeQuery(
                "SELECT COALESCE(SUM(total_estimated), 0) FROM market_order " +
                "WHERE investor_id = :id AND status IN ('PENDING', 'QUEUED') " +
                "AND order_type IN ('MARKET_BUY', 'LIMIT_BUY')")
                .setParameter("id", investorId)
                .getSingleResult();

        BigDecimal available = balance.getTotalBalance().subtract(reservedForOrders);
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }

        List<Position> positions = positionRepository.findByInvestorId(investorId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;
        BigDecimal unrealizedGain = BigDecimal.ZERO;

        for (Position pos : positions) {
            BigDecimal currentPrice = stockSnapshotService.findBySymbol(pos.getSymbol())
                    .map(s -> s.getCurrentPrice())
                    .orElse(pos.getAvgPurchasePrice());
            int qty = pos.getCurrentQuantity();
            BigDecimal cost = pos.getAvgPurchasePrice().multiply(BigDecimal.valueOf(qty));
            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(qty));
            totalCost = totalCost.add(cost);
            totalPortfolioValue = totalPortfolioValue.add(marketValue);
            unrealizedGain = unrealizedGain.add(marketValue.subtract(cost));
        }

        BigDecimal unrealizedGainPercent = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedGainPercent = unrealizedGain.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new BalanceSummaryResponse(
                available,
                reservedForOrders,
                totalCost,
                totalPortfolioValue,
                unrealizedGain,
                unrealizedGainPercent
        );
    }
}
