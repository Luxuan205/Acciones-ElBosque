package com.accioneselbosque.orders.service;

import com.accioneselbosque.orders.repository.CommissionRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CommissionCalculatorService {

    private final CommissionRateRepository commissionRateRepository;

    @Transactional(readOnly = true)
    public BigDecimal getRatePercent(String subscriptionType) {
        return commissionRateRepository.findById(subscriptionType)
                .orElseThrow(() -> new IllegalStateException("Commission rate not configured for: " + subscriptionType))
                .getRatePercent();
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCommission(BigDecimal grossValue, String subscriptionType) {
        BigDecimal ratePercent = getRatePercent(subscriptionType);
        return grossValue.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
