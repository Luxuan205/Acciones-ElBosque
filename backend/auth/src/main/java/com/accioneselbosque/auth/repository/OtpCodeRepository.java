package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByInvestorIdAndUsedAtIsNullAndExpiresAtAfter(Long investorId, LocalDateTime now);

    Optional<OtpCode> findTopByInvestorIdOrderByCreatedAtDesc(Long investorId);

    long countByInvestorIdAndCreatedAtAfter(Long investorId, LocalDateTime since);
}
