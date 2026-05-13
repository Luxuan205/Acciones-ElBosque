package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.config.SecurityConfig;
import com.accioneselbosque.auth.dto.RegisterRequest;
import com.accioneselbosque.auth.dto.RegisterResponse;
import com.accioneselbosque.auth.exception.DuplicateDocumentException;
import com.accioneselbosque.auth.exception.DuplicateEmailException;
import com.accioneselbosque.auth.exception.TokenExpiredException;
import com.accioneselbosque.auth.service.RegistrationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=TestSecretThatIsAtLeast256BitsLong1234567890ABCDEF")
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationService registrationService;

    private RegisterRequest validRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Juan Diego González");
        req.setDocumentNumber("1020304050");
        req.setEmail("juan@ejemplo.com");
        req.setPassword("Segura123");
        req.setConfirmPassword("Segura123");
        return req;
    }

    // --- US1: POST /auth/register happy path ---

    @Test
    void register_withValidData_returns201() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(new RegisterResponse(
                        "Registro exitoso. Revisa tu correo para verificar tu cuenta.",
                        "juan@ejemplo.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registro exitoso. Revisa tu correo para verificar tu cuenta."))
                .andExpect(jsonPath("$.email").value("juan@ejemplo.com"));
    }

    // --- US1: GET /auth/verify happy path ---

    @Test
    void verify_withValidToken_returns200() throws Exception {
        mockMvc.perform(get("/auth/verify").param("token", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta verificada exitosamente. Ya puedes iniciar sesión."));
    }

    // --- US2: POST /auth/register validation errors ---

    @Test
    void register_withMissingFullName_returns400WithDetails() throws Exception {
        RegisterRequest req = validRequest();
        req.setFullName("");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos de registro inválidos"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void register_withWeakPassword_returns400WithDetails() throws Exception {
        RegisterRequest req = validRequest();
        req.setPassword("sinmayus1");
        req.setConfirmPassword("sinmayus1");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos de registro inválidos"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void register_withPasswordMismatch_returns400() throws Exception {
        RegisterRequest req = validRequest();
        req.setConfirmPassword("OtroPass123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        when(registrationService.register(any())).thenThrow(new DuplicateEmailException("juan@ejemplo.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El correo ya está registrado. ¿Desea iniciar sesión?"));
    }

    @Test
    void register_withDuplicateDocument_returns409() throws Exception {
        when(registrationService.register(any())).thenThrow(new DuplicateDocumentException("1020304050"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El número de documento ya tiene una cuenta asociada."));
    }

    // --- US2: GET /auth/verify error cases ---

    @Test
    void verify_withExpiredToken_returns400() throws Exception {
        doThrow(new TokenExpiredException()).when(registrationService).verifyAccount(anyString());

        mockMvc.perform(get("/auth/verify").param("token", "expired-token-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El enlace de verificación ha expirado. Solicita uno nuevo."));
    }

    // --- US2: POST /auth/resend-verification ---

    @Test
    void resendVerification_returns200() throws Exception {
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "juan@ejemplo.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resendVerification_rateLimited_returns429() throws Exception {
        doThrow(new com.accioneselbosque.auth.exception.ResendRateLimitException())
                .when(registrationService).resendVerification(anyString());

        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "juan@ejemplo.com"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists());
    }
}
