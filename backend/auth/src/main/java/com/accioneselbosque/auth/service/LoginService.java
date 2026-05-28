package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.LoginRequest;
import com.accioneselbosque.auth.dto.LoginResponse;
import com.accioneselbosque.auth.exception.AccountDeletedException;
import com.accioneselbosque.auth.exception.AccountLockedException;
import com.accioneselbosque.auth.exception.AccountSuspendedException;
import com.accioneselbosque.auth.exception.InvalidCredentialsException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.InvestorRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final InvestorRepository investorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    // Dummy bcrypt hash for constant-time verification (prevents email enumeration timing attack)
    private static final String DUMMY_HASH = "$2a$12$dummyhashfortimingXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Value("${app.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.login.lock-minutes:30}")
    private int lockMinutes;

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
            case BLOCKED -> throw new AccountLockedException();
            case SUSPENDED -> throw new AccountSuspendedException();
            case DELETED -> throw new AccountDeletedException();
            default -> {
                // ACTIVE, PENDING, INACTIVE: proceed
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

        String role = investor.getRole() != null ? investor.getRole().name() : "INVESTOR";
        String accessToken = jwtService.generateToken(investor.getId(), role);

        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.AUTH_SUCCESS)
                .investorId(investor.getId())
                .result(AuditResult.SUCCESS)
                .detail("role=" + role)
                .build());

        return new LoginResponse(accessToken, role);
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
