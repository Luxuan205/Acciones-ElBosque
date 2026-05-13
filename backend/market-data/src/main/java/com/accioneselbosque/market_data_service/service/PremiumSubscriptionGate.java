package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.SubscriptionType;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.market_data_service.exception.PremiumRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PremiumSubscriptionGate {

    private final InvestorRepository investorRepository;

    public void assertIsPremiumActive(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found: " + investorId));

        boolean isPremium = investor.getSubscriptionType() == SubscriptionType.PREMIUM
                && investor.getSubscriptionExpiresAt() != null
                && investor.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());

        if (!isPremium) {
            throw new PremiumRequiredException();
        }
    }
}
