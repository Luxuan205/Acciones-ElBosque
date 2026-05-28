package com.accioneselbosque.app.controller;

import com.accioneselbosque.app.service.DeleteAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AccountController {

    private final DeleteAccountService deleteAccountService;

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        deleteAccountService.deleteAccount(investorId);
        return ResponseEntity.noContent().build();
    }
}
