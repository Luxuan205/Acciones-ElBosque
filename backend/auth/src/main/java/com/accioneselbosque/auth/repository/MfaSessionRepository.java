package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.MfaSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MfaSessionRepository extends JpaRepository<MfaSession, Long> {

    Optional<MfaSession> findBySessionTokenAndExpiresAtAfterAndCompletedFalse(String sessionToken, LocalDateTime now);

    void deleteByInvestorId(Long investorId);
}
