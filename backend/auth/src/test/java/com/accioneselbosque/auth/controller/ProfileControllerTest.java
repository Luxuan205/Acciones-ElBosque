package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.*;
import com.accioneselbosque.auth.exception.GlobalExceptionHandler;
import com.accioneselbosque.auth.exception.InvalidCurrentPasswordException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Language;
import com.accioneselbosque.auth.model.NotifChannel;
import com.accioneselbosque.auth.service.PasswordChangeService;
import com.accioneselbosque.auth.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private PasswordChangeService passwordChangeService;

    private MockMvc mockMvc;

    private final ObjectMapper requestMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final UsernamePasswordAuthenticationToken AUTH_1 =
            UsernamePasswordAuthenticationToken.authenticated("1", null, List.of());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProfileController(profileService, passwordChangeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    // ─── US1: GET /auth/profile ───────────────────────────────────────────────

    @Test
    void getProfile_authenticated_returns200WithProfile() throws Exception {
        ProfileResponse response = new ProfileResponse(
                1L, "Test User", "test@example.com", "1234567890",
                "+57 300 000 0000", AccountStatus.ACTIVE, LocalDateTime.now());
        when(profileService.getProfile(1L)).thenReturn(response);

        mockMvc.perform(get("/auth/profile").principal(AUTH_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    // ─── US1: PUT /auth/profile/personal ─────────────────────────────────────

    @Test
    void updatePersonalData_validRequest_returns200() throws Exception {
        UpdatePersonalDataRequest request = new UpdatePersonalDataRequest("New Name", null);
        ProfileResponse response = new ProfileResponse(
                1L, "New Name", "test@example.com", "1234567890",
                null, AccountStatus.ACTIVE, LocalDateTime.now());
        when(profileService.updatePersonalData(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/auth/profile/personal")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void updatePersonalData_invalidPhone_returns400() throws Exception {
        UpdatePersonalDataRequest request = new UpdatePersonalDataRequest(null, "not-a-phone!!!");

        mockMvc.perform(put("/auth/profile/personal")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US2: PUT /auth/change-password ──────────────────────────────────────

    @Test
    void changePassword_validRequest_returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "newPassword1", "newPassword1");

        mockMvc.perform(put("/auth/change-password")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrongPass", "newPassword1", "newPassword1");
        doThrow(new InvalidCurrentPasswordException())
                .when(passwordChangeService).changePassword(anyLong(), any());

        mockMvc.perform(put("/auth/change-password")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_mismatchedPasswords_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "newPassword1", "differentPassword");

        mockMvc.perform(put("/auth/change-password")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_tooShortNewPassword_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass1", "short", "short");

        mockMvc.perform(put("/auth/change-password")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US3: GET /auth/preferences ──────────────────────────────────────────

    @Test
    void getPreferences_firstAccess_returnsDefaults() throws Exception {
        PreferencesResponse response = new PreferencesResponse(
                NotifChannel.EMAIL, Language.es, LocalDateTime.now());
        when(profileService.getOrCreatePreferences(1L)).thenReturn(response);

        mockMvc.perform(get("/auth/preferences").principal(AUTH_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifChannel").value("EMAIL"))
                .andExpect(jsonPath("$.language").value("es"));
    }

    // ─── US3: PUT /auth/preferences ──────────────────────────────────────────

    @Test
    void updatePreferences_validRequest_returns200() throws Exception {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest(NotifChannel.SMS, Language.en);
        PreferencesResponse response = new PreferencesResponse(
                NotifChannel.SMS, Language.en, LocalDateTime.now());
        when(profileService.updatePreferences(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/auth/preferences")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifChannel").value("SMS"));
    }

    @Test
    void updatePreferences_invalidNotifChannel_returns400() throws Exception {
        mockMvc.perform(put("/auth/preferences")
                        .principal(AUTH_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifChannel\":\"INVALID\",\"language\":\"es\"}"))
                .andExpect(status().isBadRequest());
    }
}
