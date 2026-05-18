package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.SubscriptionEvent;
import com.accioneselbosque.auth.model.SubscriptionType;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.SubscriptionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryJob {

    private final InvestorRepository investorRepository;
    private final SubscriptionEventRepository subscriptionEventRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Investor> expired = investorRepository
                .findBySubscriptionTypeAndSubscriptionExpiresAtBefore(
                        SubscriptionType.PREMIUM, LocalDateTime.now());

        log.info("Expiry job: found {} expired premium subscriptions", expired.size());

        for (Investor investor : expired) {
            subscriptionEventRepository.save(SubscriptionEvent.builder()
                    .investorId(investor.getId())
                    .eventType("EXPIRED")
                    .previousType("PREMIUM")
                    .newType("STANDARD")
                    .expiresAt(null)
                    .triggeredBy("SYSTEM_JOB")
                    .createdAt(LocalDateTime.now())
                    .build());

            investor.setSubscriptionType(SubscriptionType.STANDARD);
            investor.setSubscriptionExpiresAt(null);
            investorRepository.save(investor);
        }
    }
}
