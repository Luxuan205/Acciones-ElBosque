package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByInvestorIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long investorId, LocalDate from, LocalDate to);

    Optional<PortfolioSnapshot> findByInvestorIdAndSnapshotDate(Long investorId, LocalDate date);
}
