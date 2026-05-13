package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findTopByInvestorOrderByCreatedAtDesc(Investor investor);
}
