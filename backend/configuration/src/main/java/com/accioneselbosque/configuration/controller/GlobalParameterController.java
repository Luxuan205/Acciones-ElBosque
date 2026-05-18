package com.accioneselbosque.configuration.controller;

import com.accioneselbosque.configuration.dto.GlobalParameterDto;
import com.accioneselbosque.configuration.dto.GroupedParametersResponse;
import com.accioneselbosque.configuration.dto.ParameterChangeHistoryDto;
import com.accioneselbosque.configuration.dto.UpdateParameterRequest;
import com.accioneselbosque.configuration.service.GlobalParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config/parameters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GlobalParameterController {

    private final GlobalParameterService globalParameterService;

    @GetMapping
    public ResponseEntity<GroupedParametersResponse> getAllParameters() {
        return ResponseEntity.ok(globalParameterService.getAllGroupedByCategory());
    }

    @PutMapping("/{key}")
    public ResponseEntity<GlobalParameterDto> updateParameter(
            @PathVariable String key,
            @Valid @RequestBody UpdateParameterRequest req,
            Authentication authentication) {
        Long adminId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(globalParameterService.updateParameter(key, req, adminId));
    }

    @GetMapping("/{key}/history")
    public ResponseEntity<List<ParameterChangeHistoryDto>> getHistory(@PathVariable String key) {
        return ResponseEntity.ok(globalParameterService.getHistory(key));
    }

    @PostMapping("/{key}/revert")
    public ResponseEntity<GlobalParameterDto> revertParameter(
            @PathVariable String key,
            Authentication authentication) {
        Long adminId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(globalParameterService.revertParameter(key, adminId));
    }
}
