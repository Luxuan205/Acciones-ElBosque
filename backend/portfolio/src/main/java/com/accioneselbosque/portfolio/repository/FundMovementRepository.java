package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.FundMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface FundMovementRepository extends JpaRepository<FundMovement, Long> {

    Page<FundMovement> findByInvestorIdAndCreatedAtBetween(
            Long investorId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<FundMovement> findByInvestorId(Long investorId, Pageable pageable);
}
