package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.AdminUserDetailDto;
import com.accioneselbosque.auth.dto.AdminUserDto;
import com.accioneselbosque.auth.dto.PagedUsersResponse;
import com.accioneselbosque.auth.dto.UpdateUserRoleRequest;
import com.accioneselbosque.auth.dto.UpdateUserStatusRequest;
import com.accioneselbosque.auth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PagedUsersResponse> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subscriptionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminUserService.searchUsers(email, name, status, subscriptionType, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest req,
            Authentication authentication) {
        Long adminId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adminUserService.updateUserStatus(adminId, id, req));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest req,
            Authentication authentication) {
        Long adminId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adminUserService.updateUserRole(adminId, id, req));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = Long.parseLong(authentication.getName());
        adminUserService.initiatePasswordReset(adminId, id);
        return ResponseEntity.ok(
                Map.of("message", "Password reset email sent to user's registered address"));
    }
}
