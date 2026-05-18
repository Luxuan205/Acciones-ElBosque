package com.accioneselbosque.orders.repository;

import com.accioneselbosque.orders.model.TitleReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TitleReservationRepository extends JpaRepository<TitleReservation, Long> {

    List<TitleReservation> findByInvestorIdAndSymbolAndReleasedFalse(Long investorId, String symbol);

    Optional<TitleReservation> findByOrderId(Long orderId);
}
