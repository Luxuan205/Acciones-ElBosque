package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.portfolio.model.Position;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.model.TransactionType;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import com.accioneselbosque.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PositionUpdateService {

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void onOrderExecuted(
            Long investorId,
            String symbol,
            TransactionType type,
            int quantity,
            BigDecimal executionPrice,
            BigDecimal commission,
            Long orderId) {

        Position position = positionRepository.findByInvestorIdAndSymbol(investorId, symbol)
                .orElseGet(() -> Position.builder()
                        .investorId(investorId)
                        .symbol(symbol)
                        .currentQuantity(0)
                        .avgPurchasePrice(BigDecimal.ZERO)
                        .cashBalance(BigDecimal.ZERO)
                        .build());

        BigDecimal grossAmount = executionPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal netAmount;
        BigDecimal realizedGain = null;

        if (type == TransactionType.BUY) {
            int oldQty = position.getCurrentQuantity();
            BigDecimal oldAvg = position.getAvgPurchasePrice();

            BigDecimal newAvgPrice;
            int newQty = oldQty + quantity;
            if (newQty == 0) {
                newAvgPrice = BigDecimal.ZERO;
            } else {
                BigDecimal oldTotal = oldAvg.multiply(BigDecimal.valueOf(oldQty));
                BigDecimal newTotal = executionPrice.multiply(BigDecimal.valueOf(quantity));
                newAvgPrice = oldTotal.add(newTotal)
                        .divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP);
            }

            position.setAvgPurchasePrice(newAvgPrice);
            position.setCurrentQuantity(newQty);
            netAmount = grossAmount.add(commission);
        } else {
            BigDecimal avgPrice = position.getAvgPurchasePrice();
            realizedGain = executionPrice.subtract(avgPrice).multiply(BigDecimal.valueOf(quantity));

            int newQty = position.getCurrentQuantity() - quantity;
            position.setCurrentQuantity(Math.max(0, newQty));
            netAmount = grossAmount.subtract(commission);
        }

        positionRepository.save(position);

        Transaction tx = Transaction.builder()
                .investorId(investorId)
                .orderId(orderId)
                .transactionType(type)
                .symbol(symbol)
                .quantity(quantity)
                .executionPrice(executionPrice)
                .commission(commission)
                .grossAmount(grossAmount)
                .netAmount(netAmount)
                .realizedGain(realizedGain)
                .avgPriceAtTime(position.getAvgPurchasePrice())
                .build();

        transactionRepository.save(tx);
    }
}
