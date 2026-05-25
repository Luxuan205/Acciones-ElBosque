package com.accioneselbosque.auth.service;

import com.accioneselbosque.audit.service.AuditService;
import com.accioneselbosque.auth.dto.ChangePasswordRequest;
import com.accioneselbosque.auth.exception.InvalidCurrentPasswordException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.ProfileChangeLog;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.ProfileChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private ProfileChangeLogRepository changeLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PasswordChangeService passwordChangeService;

    private Investor investor;

    @BeforeEach
    void setUp() {
        investor = new Investor();
        investor.setId(1L);
        investor.setPasswordHash("$2a$12$hashedCurrentPassword");
        investor.setAccountStatus(AccountStatus.ACTIVE);
    }

    @Test
    void changePassword_validRequest_updatesPasswordAndCreatesAuditLog() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(passwordEncoder.matches("currentPass1", investor.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("$2a$12$newHashedPassword");
        when(investorRepository.save(any())).thenReturn(investor);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "newPassword1", "newPassword1");
        passwordChangeService.changePassword(1L, request);

        verify(investorRepository).save(investor);
        assertThat(investor.getPasswordHash()).isEqualTo("$2a$12$newHashedPassword");

        ArgumentCaptor<ProfileChangeLog> logCaptor = ArgumentCaptor.forClass(ProfileChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFieldName()).isEqualTo("password");
        assertThat(logCaptor.getValue().getOldValue()).isEqualTo("[REDACTED]");
        assertThat(logCaptor.getValue().getNewValue()).isEqualTo("[REDACTED]");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCurrentPasswordException() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(passwordEncoder.matches("wrongPass", investor.getPasswordHash())).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrongPass", "newPassword1", "newPassword1");

        assertThatThrownBy(() -> passwordChangeService.changePassword(1L, request))
                .isInstanceOf(InvalidCurrentPasswordException.class);

        verify(investorRepository, never()).save(any());
        verify(changeLogRepository, never()).save(any());
    }

    @Test
    void changePassword_mismatchedPasswords_throwsIllegalArgumentException() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(passwordEncoder.matches("currentPass1", investor.getPasswordHash())).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "newPassword1", "differentPassword");

        assertThatThrownBy(() -> passwordChangeService.changePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(investorRepository, never()).save(any());
    }

    @Test
    void changePassword_auditLogContainsRedactedValues() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$newHash");
        when(investorRepository.save(any())).thenReturn(investor);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "newPassword1", "newPassword1");
        passwordChangeService.changePassword(1L, request);

        ArgumentCaptor<ProfileChangeLog> logCaptor = ArgumentCaptor.forClass(ProfileChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOldValue()).isEqualTo("[REDACTED]");
        assertThat(logCaptor.getValue().getNewValue()).isEqualTo("[REDACTED]");
    }
}
