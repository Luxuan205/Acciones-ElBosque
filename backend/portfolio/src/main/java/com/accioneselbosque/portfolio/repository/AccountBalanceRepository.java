package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    Optional<AccountBalance> findByInvestorId(Long investorId);
}
