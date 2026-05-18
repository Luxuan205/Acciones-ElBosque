package com.accioneselbosque.auth.facade;

import com.accioneselbosque.auth.model.SubscriptionType;
import com.accioneselbosque.auth.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SubscriptionGate {

    private final InvestorRepository investorRepository;

    /**
     * Returns true if the investor has an active PREMIUM subscription,
     * or if they have BROKER or ADMIN role (unrestricted).
     * Always returns false for non-existent investors.
     *
     * Note: role field doesn't exist yet (added in feature 026).
     * For now, checks subscriptionType and expiry only.
     */
    public boolean isPremiumActive(Long investorId) {
        return investorRepository.findById(investorId)
                .map(investor -> {
                    if (investor.getSubscriptionType() == SubscriptionType.PREMIUM) {
                        return investor.getSubscriptionExpiresAt() != null
                                && investor.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());
                    }
                    return false;
                })
                .orElse(false);
    }
}
