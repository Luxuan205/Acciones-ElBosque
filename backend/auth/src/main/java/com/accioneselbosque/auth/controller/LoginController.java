package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.LoginRequest;
import com.accioneselbosque.auth.dto.LoginResponse;
import com.accioneselbosque.auth.dto.MfaVerifyRequest;
import com.accioneselbosque.auth.dto.MfaVerifyResponse;
import com.accioneselbosque.auth.dto.ResendOtpRequest;
import com.accioneselbosque.auth.service.LoginService;
import com.accioneselbosque.auth.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final MfaService mfaService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(loginService.login(req));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest req) {
        return ResponseEntity.ok(mfaService.verify(req));
    }

    @PostMapping("/mfa/resend")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        return ResponseEntity.ok(mfaService.resend(req.sessionToken()));
    }
}
