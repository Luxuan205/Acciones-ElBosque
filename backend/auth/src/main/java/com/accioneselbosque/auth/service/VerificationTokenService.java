package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.exception.TokenAlreadyUsedException;
import com.accioneselbosque.auth.exception.TokenExpiredException;
import com.accioneselbosque.auth.exception.TokenNotFoundException;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.VerificationToken;
import com.accioneselbosque.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;

    @Transactional
    public VerificationToken createToken(Investor investor) {
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setInvestor(investor);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        token.setUsed(false);
        return tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public VerificationToken validateToken(String tokenValue) {
        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(TokenNotFoundException::new);

        if (token.isUsed()) {
            throw new TokenAlreadyUsedException();
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException();
        }
        return token;
    }

    @Transactional
    public void markUsed(VerificationToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
