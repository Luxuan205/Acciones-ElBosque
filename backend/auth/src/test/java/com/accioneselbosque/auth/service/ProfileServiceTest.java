package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.*;
import com.accioneselbosque.auth.exception.InvalidCurrentPasswordException;
import com.accioneselbosque.auth.model.*;
import com.accioneselbosque.auth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private InvestorPreferencesRepository preferencesRepository;

    @Mock
    private ProfileChangeLogRepository changeLogRepository;

    @InjectMocks
    private ProfileService profileService;

    private Investor investor;

    @BeforeEach
    void setUp() {
        investor = new Investor();
        investor.setId(1L);
        investor.setFullName("Original Name");
        investor.setEmail("test@example.com");
        investor.setDocumentNumber("1234567890");
        investor.setPhone(null);
        investor.setAccountStatus(AccountStatus.ACTIVE);
        investor.setCreatedAt(LocalDateTime.now());
    }

    // ─── US1: getProfile ──────────────────────────────────────────────────────

    @Test
    void getProfile_returnsProfileResponse() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));

        ProfileResponse response = profileService.getProfile(1L);

        assertThat(response.investorId()).isEqualTo(1L);
        assertThat(response.fullName()).isEqualTo("Original Name");
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    // ─── US1: updatePersonalData ──────────────────────────────────────────────

    @Test
    void updatePersonalData_updatesFullNameAndCreatesLog() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(investorRepository.save(any())).thenReturn(investor);

        UpdatePersonalDataRequest request = new UpdatePersonalDataRequest("New Name", null);
        profileService.updatePersonalData(1L, request);

        verify(investorRepository).save(investor);
        ArgumentCaptor<ProfileChangeLog> logCaptor = ArgumentCaptor.forClass(ProfileChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFieldName()).isEqualTo("fullName");
        assertThat(logCaptor.getValue().getOldValue()).isEqualTo("Original Name");
        assertThat(logCaptor.getValue().getNewValue()).isEqualTo("New Name");
    }

    @Test
    void updatePersonalData_updatesPhoneAndCreatesLog() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(investorRepository.save(any())).thenReturn(investor);

        UpdatePersonalDataRequest request = new UpdatePersonalDataRequest(null, "+57 300 000 0000");
        profileService.updatePersonalData(1L, request);

        ArgumentCaptor<ProfileChangeLog> logCaptor = ArgumentCaptor.forClass(ProfileChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFieldName()).isEqualTo("phone");
    }

    @Test
    void updatePersonalData_emailNotChanged() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(investorRepository.save(any())).thenReturn(investor);

        UpdatePersonalDataRequest request = new UpdatePersonalDataRequest("New Name", null);
        profileService.updatePersonalData(1L, request);

        assertThat(investor.getEmail()).isEqualTo("test@example.com");
        assertThat(investor.getDocumentNumber()).isEqualTo("1234567890");
    }

    // ─── US3: getOrCreatePreferences ─────────────────────────────────────────

    @Test
    void getOrCreatePreferences_createsDefaultsWhenNotExists() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(preferencesRepository.findByInvestor(investor)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PreferencesResponse response = profileService.getOrCreatePreferences(1L);

        assertThat(response.notifChannel()).isEqualTo(NotifChannel.EMAIL);
        assertThat(response.language()).isEqualTo(Language.es);
        verify(preferencesRepository).save(any(InvestorPreferences.class));
    }

    @Test
    void getOrCreatePreferences_returnsExistingPreferences() {
        InvestorPreferences prefs = new InvestorPreferences();
        prefs.setInvestor(investor);
        prefs.setNotifChannel(NotifChannel.SMS);
        prefs.setLanguage(Language.en);
        prefs.setUpdatedAt(LocalDateTime.now());

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(preferencesRepository.findByInvestor(investor)).thenReturn(Optional.of(prefs));

        PreferencesResponse response = profileService.getOrCreatePreferences(1L);

        assertThat(response.notifChannel()).isEqualTo(NotifChannel.SMS);
        verify(preferencesRepository, never()).save(any());
    }

    // ─── US3: updatePreferences ───────────────────────────────────────────────

    @Test
    void updatePreferences_persistsChanges() {
        InvestorPreferences prefs = new InvestorPreferences();
        prefs.setInvestor(investor);
        prefs.setNotifChannel(NotifChannel.EMAIL);
        prefs.setLanguage(Language.es);

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(preferencesRepository.findByInvestor(investor)).thenReturn(Optional.of(prefs));
        when(preferencesRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(NotifChannel.SMS, Language.en);
        PreferencesResponse response = profileService.updatePreferences(1L, request);

        assertThat(response.notifChannel()).isEqualTo(NotifChannel.SMS);
        assertThat(response.language()).isEqualTo(Language.en);
        verify(preferencesRepository).save(prefs);
    }
}
