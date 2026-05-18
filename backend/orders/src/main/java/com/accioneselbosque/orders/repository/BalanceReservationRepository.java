package com.accioneselbosque.orders.repository;

import com.accioneselbosque.orders.model.BalanceReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BalanceReservationRepository extends JpaRepository<BalanceReservation, Long> {

    List<BalanceReservation> findByInvestorIdAndReleasedFalse(Long investorId);

    Optional<BalanceReservation> findByOrderId(Long orderId);
}
