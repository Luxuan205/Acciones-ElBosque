package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.*;
import com.accioneselbosque.auth.service.PasswordChangeService;
import com.accioneselbosque.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final PasswordChangeService passwordChangeService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.getProfile(investorId));
    }

    @PutMapping({"/profile", "/profile/personal"})
    public ResponseEntity<ProfileResponse> updatePersonalData(
            Authentication authentication,
            @Valid @RequestBody UpdatePersonalDataRequest request) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.updatePersonalData(investorId, request));
    }

    @PutMapping({"/password", "/change-password"})
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        Long investorId = Long.parseLong(authentication.getName());
        passwordChangeService.changePassword(investorId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<PreferencesResponse> getPreferences(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.getOrCreatePreferences(investorId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<PreferencesResponse> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.updatePreferences(investorId, request));
    }
}
