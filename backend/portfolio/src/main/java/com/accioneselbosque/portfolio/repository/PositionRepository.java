package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByInvestorId(Long investorId);

    Optional<Position> findByInvestorIdAndSymbol(Long investorId, String symbol);
}
