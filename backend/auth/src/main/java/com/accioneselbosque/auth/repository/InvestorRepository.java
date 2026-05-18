package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.InvestorRole;
import com.accioneselbosque.auth.model.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long>, JpaSpecificationExecutor<Investor> {

    Optional<Investor> findByEmail(String email);

    Optional<Investor> findByDocumentNumber(String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    List<Investor> findBySubscriptionTypeAndSubscriptionExpiresAtBefore(
            SubscriptionType subscriptionType, LocalDateTime expiresAt);

    long countByAccountStatus(AccountStatus accountStatus);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countBySubscriptionTypeAndSubscriptionExpiresAtAfter(
            SubscriptionType subscriptionType, LocalDateTime expiresAt);

    long countByRoleAndAccountStatus(InvestorRole role, AccountStatus accountStatus);
}
