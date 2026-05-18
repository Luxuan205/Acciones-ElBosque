package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    Optional<SubscriptionEvent> findTopByInvestorIdOrderByCreatedAtDesc(Long investorId);
}
