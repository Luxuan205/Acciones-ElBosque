package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByInvestorId(Long investorId);

    Optional<Position> findByInvestorIdAndSymbol(Long investorId, String symbol);

    @Query("SELECT DISTINCT p.investorId FROM Position p WHERE p.currentQuantity > 0")
    List<Long> findDistinctInvestorIds();
}
