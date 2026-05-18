package com.accioneselbosque.notifications.repository;

import com.accioneselbosque.notifications.model.MarketAlertSubscription;
import com.accioneselbosque.notifications.model.MarketAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketAlertSubscriptionRepository extends JpaRepository<MarketAlertSubscription, Long> {

    List<MarketAlertSubscription> findByInvestorIdAndActiveTrue(Long investorId);

    List<MarketAlertSubscription> findByAlertTypeAndActive(MarketAlertType alertType, boolean active);

    Optional<MarketAlertSubscription> findByIdAndInvestorId(Long id, Long investorId);
}
