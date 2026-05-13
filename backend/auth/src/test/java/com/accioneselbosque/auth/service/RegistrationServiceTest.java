package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.RegisterRequest;
import com.accioneselbosque.auth.dto.RegisterResponse;
import com.accioneselbosque.auth.exception.DuplicateDocumentException;
import com.accioneselbosque.auth.exception.DuplicateEmailException;
import com.accioneselbosque.auth.exception.ResendRateLimitException;
import com.accioneselbosque.auth.exception.TokenExpiredException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.VerificationToken;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    private RegisterRequest validRequest;
    private Investor savedInvestor;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setFullName("Juan Diego González");
        validRequest.setDocumentNumber("1020304050");
        validRequest.setEmail("juan@ejemplo.com");
        validRequest.setPassword("Segura123");
        validRequest.setConfirmPassword("Segura123");

        savedInvestor = new Investor();
        savedInvestor.setId(1L);
        savedInvestor.setEmail("juan@ejemplo.com");
        savedInvestor.setAccountStatus(AccountStatus.PENDING);
    }

    @Test
    void register_withValidData_savesInvestorAndSendsEmail() {
        when(investorRepository.existsByEmail(anyString())).thenReturn(false);
        when(investorRepository.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(investorRepository.save(any())).thenReturn(savedInvestor);

        VerificationToken token = new VerificationToken();
        token.setToken("550e8400-e29b-41d4-a716-446655440000");
        when(verificationTokenService.createToken(any())).thenReturn(token);
        doNothing().when(mailService).sendVerificationEmail(any(), anyString());

        RegisterResponse response = registrationService.register(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("juan@ejemplo.com");
        verify(investorRepository).save(any(Investor.class));
        verify(verificationTokenService).createToken(any(Investor.class));
        verify(mailService).sendVerificationEmail(any(Investor.class), anyString());
    }

    @Test
    void register_withDuplicateEmail_throwsDuplicateEmailException() {
        when(investorRepository.existsByEmail("juan@ejemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(DuplicateEmailException.class);

        verify(investorRepository, never()).save(any());
    }

    @Test
    void register_withDuplicateDocument_throwsDuplicateDocumentException() {
        when(investorRepository.existsByEmail(anyString())).thenReturn(false);
        when(investorRepository.existsByDocumentNumber("1020304050")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(investorRepository, never()).save(any());
    }

    @Test
    void verifyAccount_withExpiredToken_throwsTokenExpiredException() {
        doThrow(new TokenExpiredException()).when(verificationTokenService).validateToken("some-token");

        assertThatThrownBy(() -> registrationService.verifyAccount("some-token"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void resendVerification_withRecentToken_throwsResendRateLimitException() {
        VerificationToken recentToken = new VerificationToken();
        recentToken.setCreatedAt(LocalDateTime.now().minusSeconds(30));
        recentToken.setInvestor(savedInvestor);

        savedInvestor.setAccountStatus(AccountStatus.PENDING);

        when(investorRepository.findByEmail("juan@ejemplo.com")).thenReturn(Optional.of(savedInvestor));
        when(tokenRepository.findTopByInvestorOrderByCreatedAtDesc(savedInvestor))
                .thenReturn(Optional.of(recentToken));

        assertThatThrownBy(() -> registrationService.resendVerification("juan@ejemplo.com"))
                .isInstanceOf(ResendRateLimitException.class);
    }

    @Test
    void resendVerification_withNonExistentEmail_returnsWithoutError() {
        when(investorRepository.findByEmail("noexiste@ejemplo.com")).thenReturn(Optional.empty());

        registrationService.resendVerification("noexiste@ejemplo.com");

        verify(mailService, never()).sendVerificationEmail(any(), anyString());
    }
}
