package com.accioneselbosque.configuration.controller;

import com.accioneselbosque.configuration.dto.MarketHolidayDto;
import com.accioneselbosque.configuration.dto.MarketScheduleDto;
import com.accioneselbosque.configuration.dto.MarketStatusDto;
import com.accioneselbosque.configuration.service.MarketScheduleService;
import com.accioneselbosque.configuration.service.MarketStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/config/market")
@RequiredArgsConstructor
public class MarketConfigController {

    private final MarketStatusService statusService;
    private final MarketScheduleService scheduleService;

    @GetMapping("/status")
    public ResponseEntity<MarketStatusDto> getStatus() {
        return ResponseEntity.ok(statusService.getStatus());
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketScheduleDto> getSchedule() {
        return ResponseEntity.ok(scheduleService.getSchedule());
    }

    @PutMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketScheduleDto> updateSchedule(
            @Valid @RequestBody MarketScheduleDto dto,
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(scheduleService.updateSchedule(dto, adminId));
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, List<MarketHolidayDto>>> listHolidays(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : Year.now().getValue();
        return ResponseEntity.ok(Map.of("holidays", scheduleService.listHolidays(targetYear)));
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketHolidayDto> addHoliday(
            @Valid @RequestBody MarketHolidayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.addHoliday(dto));
    }

    @DeleteMapping("/holidays/{holidayId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable UUID holidayId) {
        scheduleService.deleteHoliday(holidayId);
        return ResponseEntity.noContent().build();
    }
}
