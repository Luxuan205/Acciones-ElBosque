package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.RegisterRequest;
import com.accioneselbosque.auth.dto.RegisterResponse;
import com.accioneselbosque.auth.exception.AccountAlreadyActiveException;
import com.accioneselbosque.auth.exception.DuplicateDocumentException;
import com.accioneselbosque.auth.exception.DuplicateEmailException;
import com.accioneselbosque.auth.exception.ResendRateLimitException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.VerificationToken;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final InvestorRepository investorRepository;
    private final VerificationTokenRepository tokenRepository;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (investorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }
        if (investorRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateDocumentException(request.getDocumentNumber());
        }

        Investor investor = new Investor();
        investor.setFullName(request.getFullName());
        investor.setDocumentNumber(request.getDocumentNumber());
        investor.setEmail(request.getEmail());
        investor.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        investor.setAccountStatus(AccountStatus.ACTIVE);

        investor = investorRepository.save(investor);

        VerificationToken token = verificationTokenService.createToken(investor);
        mailService.sendVerificationEmail(investor, token.getToken());

        return new RegisterResponse(
                "Registro exitoso. Revisa tu correo para verificar tu cuenta.",
                investor.getEmail()
        );
    }

    @Transactional
    public void verifyAccount(String tokenValue) {
        VerificationToken token = verificationTokenService.validateToken(tokenValue);
        Investor investor = token.getInvestor();
        investor.setAccountStatus(AccountStatus.ACTIVE);
        investorRepository.save(investor);
        verificationTokenService.markUsed(token);
    }

    @Transactional
    public void resendVerification(String email) {
        Optional<Investor> investorOpt = investorRepository.findByEmail(email);

        if (investorOpt.isEmpty()) {
            return;
        }

        Investor investor = investorOpt.get();

        if (investor.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new AccountAlreadyActiveException();
        }

        Optional<VerificationToken> lastToken =
                tokenRepository.findTopByInvestorOrderByCreatedAtDesc(investor);

        if (lastToken.isPresent()) {
            LocalDateTime rateLimitBoundary = lastToken.get().getCreatedAt().plusMinutes(2);
            if (rateLimitBoundary.isAfter(LocalDateTime.now())) {
                throw new ResendRateLimitException();
            }
        }

        VerificationToken token = verificationTokenService.createToken(investor);
        mailService.sendVerificationEmail(investor, token.getToken());
    }
}
