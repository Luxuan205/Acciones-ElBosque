package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.MfaVerifyRequest;
import com.accioneselbosque.auth.dto.MfaVerifyResponse;
import com.accioneselbosque.auth.exception.InvalidOtpException;
import com.accioneselbosque.auth.exception.MfaSessionExpiredException;
import com.accioneselbosque.auth.exception.ResendLimitException;
import com.accioneselbosque.auth.model.MfaSession;
import com.accioneselbosque.auth.model.OtpCode;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.MfaSessionRepository;
import com.accioneselbosque.auth.repository.OtpCodeRepository;
import com.accioneselbosque.audit.model.AuditEventRecord;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private final MfaSessionRepository mfaSessionRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final InvestorRepository investorRepository;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AuditService auditService;

    @Value("${app.login.otp-ttl-minutes:5}")
    private int otpTtlMinutes;

    @Value("${app.login.max-resends:3}")
    private int maxResends;

    @Value("${app.login.resend-window-minutes:5}")
    private int resendWindowMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    public MfaVerifyResponse verify(MfaVerifyRequest req) {
        MfaSession session = mfaSessionRepository
                .findBySessionTokenAndExpiresAtAfterAndCompletedFalse(req.sessionToken(), LocalDateTime.now())
                .orElseThrow(MfaSessionExpiredException::new);

        var validOtps = otpCodeRepository
                .findByInvestorIdAndUsedAtIsNullAndExpiresAtAfter(session.getInvestorId(), LocalDateTime.now());

        var matchingOtpOpt = validOtps.stream()
                .filter(o -> o.getCode().equals(req.otpCode()))
                .findFirst();

        if (matchingOtpOpt.isEmpty()) {
            // CRIT-2: Invalidate session immediately on first wrong OTP to prevent brute-force
            session.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            mfaSessionRepository.save(session);
            auditService.record(AuditEventRecord.builder()
                    .eventType(AuditEventType.AUTH_MFA_FAILED)
                    .investorId(session.getInvestorId())
                    .result(AuditResult.FAILURE)
                    .detail("invalid OTP, session invalidated")
                    .build());
            throw new InvalidOtpException();
        }

        OtpCode otp = matchingOtpOpt.get();

        // Mark OTP as used
        otp.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);

        // Mark session as completed
        session.setCompleted(true);
        mfaSessionRepository.save(session);

        String role = investorRepository.findById(session.getInvestorId())
                .map(inv -> inv.getRole() != null ? inv.getRole().name() : "INVESTOR")
                .orElse("INVESTOR");

        // Emit JWT
        String accessToken = jwtService.generateToken(session.getInvestorId(), role);
        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.AUTH_SUCCESS)
                .investorId(session.getInvestorId())
                .result(AuditResult.SUCCESS)
                .detail("role=" + role)
                .build());
        return new MfaVerifyResponse(accessToken, role);
    }

    public Map<String, String> resend(String sessionToken) {
        MfaSession session = mfaSessionRepository
                .findBySessionTokenAndExpiresAtAfterAndCompletedFalse(sessionToken, LocalDateTime.now())
                .orElseThrow(MfaSessionExpiredException::new);

        // Rate limit: max resends in last N minutes
        long recentOtps = otpCodeRepository.countByInvestorIdAndCreatedAtAfter(
                session.getInvestorId(), LocalDateTime.now().minusMinutes(resendWindowMinutes));
        if (recentOtps >= maxResends) {
            throw new ResendLimitException();
        }

        // Generate new OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpCode otp = OtpCode.builder()
                .investorId(session.getInvestorId())
                .code(otpCode)
                .channel("EMAIL")
                .expiresAt(LocalDateTime.now().plusMinutes(otpTtlMinutes))
                .createdAt(LocalDateTime.now())
                .build();
        otpCodeRepository.save(otp);

        // Get investor email and send OTP
        investorRepository.findById(session.getInvestorId())
                .ifPresent(inv -> mailService.sendOtp(inv.getEmail(), otpCode));

        return Map.of("channel", "EMAIL", "message", "Código reenviado exitosamente.");
    }
}
