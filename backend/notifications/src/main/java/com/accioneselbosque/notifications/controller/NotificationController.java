package com.accioneselbosque.notifications.controller;

import com.accioneselbosque.notifications.dto.NotificationDetailDto;
import com.accioneselbosque.notifications.dto.PagedNotificationResponse;
import com.accioneselbosque.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("hasRole('INVESTOR') or hasRole('BROKER')")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedNotificationResponse> getHistory(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(notificationService.getHistory(investorId, page, size, eventType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailDto> getDetail(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(notificationService.getDetail(investorId, id));
    }
}
