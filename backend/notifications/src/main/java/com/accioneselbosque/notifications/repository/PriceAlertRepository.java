package com.accioneselbosque.notifications.repository;

import com.accioneselbosque.notifications.model.PriceAlert;
import com.accioneselbosque.notifications.model.PriceAlertStatus;
import com.accioneselbosque.notifications.model.PriceAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    List<PriceAlert> findByStatusAndAlertType(PriceAlertStatus status, PriceAlertType type);

    List<PriceAlert> findByStatus(PriceAlertStatus status);

    Optional<PriceAlert> findByIdAndInvestorId(Long id, Long investorId);
}
