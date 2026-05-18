package com.accioneselbosque.notifications.service;

import com.accioneselbosque.notifications.dto.CreateMarketAlertRequest;
import com.accioneselbosque.notifications.dto.MarketAlertSubscriptionDto;
import com.accioneselbosque.notifications.exception.MarketAlertNotFoundException;
import com.accioneselbosque.notifications.model.MarketAlertSubscription;
import com.accioneselbosque.notifications.repository.MarketAlertSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketAlertService {

    private final MarketAlertSubscriptionRepository marketAlertSubscriptionRepository;

    @Transactional
    public MarketAlertSubscriptionDto subscribe(Long investorId, CreateMarketAlertRequest req) {
        MarketAlertSubscription subscription = MarketAlertSubscription.builder()
                .investorId(investorId)
                .alertType(req.alertType())
                .symbol(req.symbol())
                .threshold(req.threshold())
                .active(true)
                .build();
        subscription = marketAlertSubscriptionRepository.save(subscription);
        return toDto(subscription);
    }

    @Transactional(readOnly = true)
    public List<MarketAlertSubscriptionDto> getSubscriptions(Long investorId) {
        return marketAlertSubscriptionRepository.findByInvestorIdAndActiveTrue(investorId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketAlertSubscriptionDto updateSubscription(Long investorId, Long subscriptionId,
                                                         CreateMarketAlertRequest req) {
        MarketAlertSubscription subscription = marketAlertSubscriptionRepository
                .findByIdAndInvestorId(subscriptionId, investorId)
                .orElseThrow(() -> new MarketAlertNotFoundException(subscriptionId));

        subscription.setAlertType(req.alertType());
        subscription.setSymbol(req.symbol());
        subscription.setThreshold(req.threshold());
        subscription = marketAlertSubscriptionRepository.save(subscription);
        return toDto(subscription);
    }

    @Transactional
    public void deleteSubscription(Long investorId, Long subscriptionId) {
        MarketAlertSubscription subscription = marketAlertSubscriptionRepository
                .findByIdAndInvestorId(subscriptionId, investorId)
                .orElseThrow(() -> new MarketAlertNotFoundException(subscriptionId));

        subscription.setActive(false);
        marketAlertSubscriptionRepository.save(subscription);
    }

    private MarketAlertSubscriptionDto toDto(MarketAlertSubscription s) {
        return new MarketAlertSubscriptionDto(
                s.getId(),
                s.getAlertType().name(),
                s.getSymbol(),
                s.getThreshold(),
                s.isActive(),
                s.getCreatedAt()
        );
    }
}
