package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.LoginRequest;
import com.accioneselbosque.auth.dto.LoginResponse;
import com.accioneselbosque.auth.exception.AccountLockedException;
import com.accioneselbosque.auth.exception.AccountSuspendedException;
import com.accioneselbosque.auth.exception.InvalidCredentialsException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final InvestorRepository investorRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final MfaSessionRepository mfaSessionRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // Dummy bcrypt hash for constant-time verification (prevents email enumeration timing attack)
    private static final String DUMMY_HASH = "$2a$12$dummyhashfortimingXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Value("${app.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.login.lock-minutes:30}")
    private int lockMinutes;

    @Value("${app.login.otp-ttl-minutes:5}")
    private int otpTtlMinutes;

    @Value("${app.login.session-ttl-minutes:5}")
    private int sessionTtlMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    public LoginResponse login(LoginRequest req) {
        Optional<Investor> optInvestor = investorRepository.findByEmail(req.email());

        // Generic error: same message whether email not found or wrong password
        if (optInvestor.isEmpty()) {
            // CRIT-1: Perform constant-time verification even if email not found
            // to prevent email enumeration timing attacks (bcrypt takes ~100ms)
            passwordEncoder.matches(req.password(), DUMMY_HASH);
            auditService.record(AuditEventRecord.builder()
                    .eventType(AuditEventType.AUTH_FAILURE)
                    .result(AuditResult.FAILURE)
                    .detail("unknown email: " + req.email())
                    .build());
            throw new InvalidCredentialsException();
        }

        Investor investor = optInvestor.get();

        // Check auto-unlock if lock period expired
        if (investor.getAccountStatus() == AccountStatus.BLOCKED
                && investor.getLockedUntil() != null
                && investor.getLockedUntil().isBefore(LocalDateTime.now())) {
            investor.setAccountStatus(AccountStatus.ACTIVE);
            investor.setFailedAttempts(0);
            investor.setLockedUntil(null);
            investorRepository.save(investor);
        }

        // Check account status
        switch (investor.getAccountStatus()) {
            case PENDING -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING");
            case BLOCKED -> throw new AccountLockedException();
            case SUSPENDED -> throw new AccountSuspendedException();
            default -> {
                // ACTIVE or INACTIVE: proceed
            }
        }

        // Validate password
        if (!passwordEncoder.matches(req.password(), investor.getPasswordHash())) {
            int attempts = investor.getFailedAttempts() + 1;
            investor.setFailedAttempts(attempts);
            boolean nowLocked = attempts >= maxAttempts;
            if (nowLocked) {
                investor.setAccountStatus(AccountStatus.BLOCKED);
                investor.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            }
            investorRepository.save(investor);
            auditService.record(AuditEventRecord.builder()
                    .eventType(AuditEventType.AUTH_FAILURE)
                    .investorId(investor.getId())
                    .result(AuditResult.FAILURE)
                    .detail("wrong password, attempts=" + attempts)
                    .build());
            if (nowLocked) {
                auditService.record(AuditEventRecord.builder()
                        .eventType(AuditEventType.ACCOUNT_LOCKED)
                        .investorId(investor.getId())
                        .result(AuditResult.SUCCESS)
                        .detail("locked for " + lockMinutes + " minutes after " + maxAttempts + " failed attempts")
                        .build());
            }
            throw new InvalidCredentialsException();
        }

        // Login OK: reset failed attempts
        investor.setFailedAttempts(0);
        investor.setLockedUntil(null);
        investorRepository.save(investor);

        // Create MFA session
        String sessionToken = UUID.randomUUID().toString();
        MfaSession session = MfaSession.builder()
                .sessionToken(sessionToken)
                .investorId(investor.getId())
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTtlMinutes))
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        mfaSessionRepository.save(session);

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpCode otp = OtpCode.builder()
                .investorId(investor.getId())
                .code(otpCode)
                .channel("EMAIL")
                .expiresAt(LocalDateTime.now().plusMinutes(otpTtlMinutes))
                .createdAt(LocalDateTime.now())
                .build();
        otpCodeRepository.save(otp);

        // Send OTP via email
        mailService.sendOtp(investor.getEmail(), otpCode);

        return new LoginResponse(sessionToken, "EMAIL");
    }

    /**
     * Used by AdminUserService (feature 026) to unlock accounts.
     */
    public void unlockAccount(Long investorId) {
        investorRepository.findById(investorId).ifPresent(investor -> {
            investor.setAccountStatus(AccountStatus.ACTIVE);
            investor.setFailedAttempts(0);
            investor.setLockedUntil(null);
            investorRepository.save(investor);
        });
    }
}
